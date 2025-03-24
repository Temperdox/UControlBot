/*
// Main JavaScript file for the Discord Bot UI

// Constants
const getApiBaseUrl = function() {
    return window.API_BASE_URL;
};

// State
let currentState = {
    servers: [],
    selectedServerId: null,
    selectedChannelId: null,
    selectedDmUserId: null,
    isDmView: true,
    channels: [],
    dmUsers: [],
    messages: [],
    users: [],
    botUser: null,
    isConnected: false,
    webSocket: null
};

// DOM Elements
const serversList = document.getElementById('servers-container');
const channelsList = document.getElementById('channels-list');
const messagesContainer = document.getElementById('messages-container');
const usersList = document.getElementById('users-container');
const messageForm = document.getElementById('message-form');
const messageInput = document.getElementById('message-input');
const guildNameElement = document.getElementById('guild-name');
const channelInfoElement = document.getElementById('channel-info');
const connectionStatusElement = document.getElementById('connection-status');
const statusIndicator = document.querySelector('.status-indicator');

// Initialize WebSocket
function initWebSocket() {
    const wsUrl = `ws://${window.location.host}/ws`;
    const ws = new WebSocket(wsUrl);

    ws.onopen = () => {
        console.log('WebSocket connected');
        currentState.isConnected = true;
        updateConnectionStatus();
    };

    ws.onclose = () => {
        console.log('WebSocket disconnected');
        currentState.isConnected = false;
        updateConnectionStatus();

        // Attempt to reconnect after 5 seconds
        setTimeout(() => {
            initWebSocket();
        }, 5000);
    };

    ws.onmessage = (event) => {
        const message = JSON.parse(event.data);
        console.log('WebSocket message received:', message);

        // Handle different event types
        handleWebSocketEvent(message);
    };

    ws.onerror = (error) => {
        console.error('WebSocket error:', error);
        currentState.isConnected = false;
        updateConnectionStatus();
    };

    currentState.webSocket = ws;
}

// Update the connection status display
function updateConnectionStatus() {
    if (currentState.isConnected) {
        connectionStatusElement.textContent = 'Connected';
        statusIndicator.classList.add('online');
    } else {
        connectionStatusElement.textContent = 'Disconnected';
        statusIndicator.classList.remove('online');
    }
}

// Handle WebSocket events
function handleWebSocketEvent(event) {
    const { type, data } = event;

    switch (type) {
        case 'MESSAGE_RECEIVED':
            handleNewMessage(data);
            break;
        case 'USER_UPDATE':
            // Handle user update
            break;
        case 'GUILD_MEMBER_JOIN':
            // Handle member join
            break;
        // Add more event handlers as needed
    }
}

// Handle a new message received
function handleNewMessage(messageData) {
    // Check if this message belongs to the current view
    if (currentState.isDmView && messageData.author.id === currentState.selectedDmUserId) {
        // Add message to the current list and render
        currentState.messages.push(messageData);
        renderMessages();
    } else if (!currentState.isDmView && messageData.channelId === currentState.selectedChannelId) {
        // Add message to the current list and render
        currentState.messages.push(messageData);
        renderMessages();
    }
}

// Send WebSocket message
function sendWebSocketMessage(type, data) {
    if (currentState.webSocket && currentState.isConnected) {
        currentState.webSocket.send(JSON.stringify({
            type,
            data
        }));
    }
}

// API Functions
async function fetchGuilds() {
    try {
        const response = await fetch(`${getApiBaseUrl}/guilds`);
        if (!response.ok) throw new Error('Failed to fetch guilds');

        const data = await response.json();
        currentState.servers = data;
        renderServers();
    } catch (error) {
        console.error('Error fetching guilds:', error);
    }
}

async function fetchGuildChannels(guildId) {
    try {
        const response = await fetch(`${getApiBaseUrl}/guilds/${guildId}/channels`);
        if (!response.ok) throw new Error('Failed to fetch channels');

        const data = await response.json();
        currentState.channels = data;
        renderChannels();
    } catch (error) {
        console.error('Error fetching channels:', error);
    }
}

async function fetchGuildMembers(guildId) {
    try {
        const response = await fetch(`${getApiBaseUrl}/guilds/${guildId}/members`);
        if (!response.ok) throw new Error('Failed to fetch members');

        const data = await response.json();
        currentState.users = data;
        renderUsers();
    } catch (error) {
        console.error('Error fetching members:', error);
    }
}

async function fetchDMs() {
    try {
        const response = await fetch(`${getApiBaseUrl}/users?dm=true`);
        if (!response.ok) throw new Error('Failed to fetch DMs');

        const data = await response.json();
        currentState.dmUsers = data;
        renderDmList();
    } catch (error) {
        console.error('Error fetching DMs:', error);
    }
}

async function fetchMessages(channelId) {
    try {
        const response = await fetch(`${getApiBaseUrl}/channels/${channelId}/messages`);
        if (!response.ok) throw new Error('Failed to fetch messages');

        const data = await response.json();
        currentState.messages = data.messages || [];
        renderMessages();
    } catch (error) {
        console.error('Error fetching messages:', error);
    }
}

async function fetchDmMessages(userId) {
    try {
        const response = await fetch(`${getApiBaseUrl}/users/${userId}/dm`);
        if (!response.ok) throw new Error('Failed to fetch DM');

        const dmChannel = await response.json();
        if (dmChannel.id) {
            await fetchMessages(dmChannel.id);
        } else {
            currentState.messages = [];
            renderMessages();
        }
    } catch (error) {
        console.error('Error fetching DM channel:', error);
    }
}

async function sendMessage(channelId, content) {
    try {
        const response = await fetch(`${getApiBaseUrl}/channels/${channelId}/messages`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ content }),
        });

        if (!response.ok) throw new Error('Failed to send message');

        // Message sent successfully, fetch the latest messages
        if (currentState.isDmView) {
            await fetchDmMessages(currentState.selectedDmUserId);
        } else {
            await fetchMessages(currentState.selectedChannelId);
        }
    } catch (error) {
        console.error('Error sending message:', error);
    }
}

async function fetchBotUser() {
    try {
        const response = await fetch(`${getApiBaseUrl}/users/@me`);
        if (!response.ok) throw new Error('Failed to fetch bot user');

        const data = await response.json();
        currentState.botUser = data;
        updateBotInfo();
    } catch (error) {
        console.error('Error fetching bot user:', error);
    }
}

// Render Functions
function renderServers() {
    serversList.innerHTML = '';

    currentState.servers.forEach(server => {
        const serverEl = document.createElement('div');
        serverEl.className = `server-item${currentState.selectedServerId === server.id ? ' active' : ''}`;
        serverEl.dataset.id = server.id;

        let serverContent = '';
        if (server.iconUrl) {
            serverContent = `<img src="${server.iconUrl}" alt="${server.name}">`;
        } else {
            // Create initials from server name
            const initials = server.name
                .split(' ')
                .map(word => word[0])
                .join('')
                .substring(0, 2);
            serverContent = initials;
        }

        serverEl.innerHTML = `
            <div class="server-icon">${serverContent}</div>
            <div class="server-tooltip">${server.name}</div>
        `;

        serverEl.addEventListener('click', () => {
            selectServer(server.id);
        });

        serversList.appendChild(serverEl);
    });
}

// Render Functions (continued)
function renderChannels() {
    channelsList.innerHTML = '';

    // Group channels by category
    const categories = {};
    const uncategorizedChannels = [];

    currentState.channels.forEach(channel => {
        if (channel.type === 'CATEGORY') {
            categories[channel.id] = {
                ...channel,
                channels: []
            };
        } else if (channel.parentId) {
            if (categories[channel.parentId]) {
                categories[channel.parentId].channels.push(channel);
            } else {
                uncategorizedChannels.push(channel);
            }
        } else {
            uncategorizedChannels.push(channel);
        }
    });

    // Render uncategorized channels
    uncategorizedChannels.forEach(channel => {
        if (channel.type === 'TEXT' || channel.type === 'VOICE') {
            const channelEl = createChannelElement(channel);
            channelsList.appendChild(channelEl);
        }
    });

    // Render categories and their channels
    Object.values(categories).forEach(category => {
        const categoryEl = document.createElement('div');
        categoryEl.className = 'channel-category';
        categoryEl.innerHTML = `
            <span class="category-arrow">▼</span>
            <span>${category.name}</span>
        `;

        channelsList.appendChild(categoryEl);

        // Sort channels by position
        category.channels.sort((a, b) => a.position - b.position);

        // Create a container for the category's channels
        const channelsContainerEl = document.createElement('div');
        channelsContainerEl.className = 'category-channels';

        // Add channels to the container
        category.channels.forEach(channel => {
            if (channel.type === 'TEXT' || channel.type === 'VOICE') {
                const channelEl = createChannelElement(channel);
                channelsContainerEl.appendChild(channelEl);
            }
        });

        channelsList.appendChild(channelsContainerEl);

        // Add category collapse functionality
        categoryEl.addEventListener('click', () => {
            categoryEl.classList.toggle('collapsed');
            channelsContainerEl.style.display =
                categoryEl.classList.contains('collapsed') ? 'none' : 'block';
        });
    });
}

function createChannelElement(channel) {
    const channelEl = document.createElement('div');
    channelEl.className = `channel-item ${channel.type.toLowerCase()}${
        currentState.selectedChannelId === channel.id ? ' active' : ''
    }`;
    channelEl.dataset.id = channel.id;

    // Icon based on channel type
    let icon = '';
    if (channel.type === 'TEXT') {
        icon = `<span class="channel-icon">#</span>`;
    } else if (channel.type === 'VOICE') {
        icon = `<span class="channel-icon"><i class="fas fa-volume-up"></i></span>`;
    }

    channelEl.innerHTML = `
        ${icon}
        <span class="channel-name">${channel.name}</span>
    `;

    channelEl.addEventListener('click', () => {
        selectChannel(channel.id);
    });

    return channelEl;
}

function renderDmList() {
    channelsList.innerHTML = '';

    currentState.dmUsers.forEach(user => {
        const dmEl = document.createElement('div');
        dmEl.className = `dm-item${currentState.selectedDmUserId === user.id ? ' active' : ''}`;
        dmEl.dataset.id = user.id;

        let avatar = '';
        if (user.avatarUrl) {
            avatar = `<img src="${user.avatarUrl}" alt="${user.username}">`;
        } else {
            // Use first letter of username as avatar
            avatar = user.username.charAt(0);
        }

        // Get status color
        const statusClass = user.status ? user.status.toLowerCase() : 'offline';

        dmEl.innerHTML = `
            <div class="dm-avatar">
                ${avatar}
                <div class="dm-status ${statusClass}"></div>
            </div>
            <span class="dm-name">${user.username}</span>
        `;

        dmEl.addEventListener('click', () => {
            selectDmUser(user.id);
        });

        channelsList.appendChild(dmEl);
    });
}

function renderUsers() {
    usersList.innerHTML = '';

    // Group users by status
    const groups = {
        online: [],
        idle: [],
        dnd: [],
        offline: []
    };

    currentState.users.forEach(user => {
        const status = user.status || 'offline';
        if (groups[status]) {
            groups[status].push(user);
        } else {
            groups.offline.push(user);
        }
    });

    // Sort users by name within each group
    Object.keys(groups).forEach(status => {
        groups[status].sort((a, b) => {
            const nameA = a.displayName || a.username || '';
            const nameB = b.displayName || b.username || '';
            return nameA.localeCompare(nameB);
        });
    });

    // Render status groups
    const statusNames = {
        online: 'ONLINE',
        idle: 'IDLE',
        dnd: 'DO NOT DISTURB',
        offline: 'OFFLINE'
    };

    Object.keys(groups).forEach(status => {
        const users = groups[status];
        if (users.length === 0) return;

        const groupEl = document.createElement('div');
        groupEl.className = 'user-group';

        groupEl.innerHTML = `
            <div class="user-group-header">
                ${statusNames[status]} - ${users.length}
            </div>
        `;

        users.forEach(user => {
            const userEl = document.createElement('div');
            userEl.className = 'user-item';
            userEl.dataset.id = user.id;

            let avatar = '';
            if (user.avatarUrl) {
                avatar = `<img src="${user.avatarUrl}" alt="${user.displayName || user.username}">`;
            } else {
                // Use first letter of display name or username as avatar
                avatar = (user.displayName || user.username).charAt(0);
            }

            userEl.innerHTML = `
                <div class="user-avatar">
                    ${avatar}
                    <div class="user-status ${status}"></div>
                </div>
                <span class="user-name">${user.displayName || user.username}</span>
            `;

            userEl.addEventListener('click', () => {
                selectUser(user.id);
            });

            groupEl.appendChild(userEl);
        });

        usersList.appendChild(groupEl);
    });
}

function renderMessages() {
    messagesContainer.innerHTML = '';

    if (currentState.messages.length === 0) {
        messagesContainer.innerHTML = `
            <div class="welcome-message">
                <h2>No messages yet</h2>
                <p>Send a message to start the conversation!</p>
            </div>
        `;
        return;
    }

    // Group messages by author to handle continuations
    let lastAuthorId = null;

    currentState.messages.forEach((message, index) => {
        const isContinuation = lastAuthorId === message.author?.id;
        lastAuthorId = message.author?.id;

        const messageEl = document.createElement('div');
        messageEl.className = `message-item${isContinuation ? ' continuation' : ''}`;

        let avatar = '';
        if (message.author?.avatarUrl) {
            avatar = `<img src="${message.author.avatarUrl}" alt="${message.author.username}">`;
        } else if (message.author) {
            // Use first letter of username as avatar
            avatar = message.author.username.charAt(0);
        }

        const timestamp = new Date(message.timestamp).toLocaleString();

        let authorHeader = '';
        if (!isContinuation) {
            authorHeader = `
                <div class="message-avatar">
                    ${avatar}
                </div>
                <div class="message-content">
                    <div class="message-author">
                        <span class="author-name">${message.author?.username || 'Unknown User'}</span>
                        <span class="message-timestamp">${timestamp}</span>
                    </div>
            `;
        } else {
            authorHeader = `
                <div class="message-avatar" style="opacity: 0;"></div>
                <div class="message-content">
            `;
        }

        messageEl.innerHTML = `
            ${authorHeader}
            <div class="message-text">${message.content}</div>
            </div>
        `;

        messagesContainer.appendChild(messageEl);
    });

    // Scroll to bottom
    messagesContainer.scrollTop = messagesContainer.scrollHeight;
}

function updateBotInfo() {
    if (!currentState.botUser) return;

    const botName = document.querySelector('.bot-name');
    const botDiscriminator = document.querySelector('.bot-discriminator');
    const botAvatar = document.querySelector('.bot-avatar');

    botName.textContent = currentState.botUser.username;

    if (currentState.botUser.discriminator && currentState.botUser.discriminator !== '0') {
        botDiscriminator.textContent = `#${currentState.botUser.discriminator}`;
    } else {
        botDiscriminator.textContent = '';
    }

    if (currentState.botUser.avatarUrl) {
        botAvatar.innerHTML = `<img src="${currentState.botUser.avatarUrl}" alt="${currentState.botUser.username}">`;
    } else {
        botAvatar.innerHTML = `<div class="default-avatar">${currentState.botUser.username.charAt(0)}</div>`;
    }
}

// Selection Functions
function selectServer(serverId) {
    // Deactivate previous selection
    const prevSelectedServer = document.querySelector('.server-item.active');
    if (prevSelectedServer) {
        prevSelectedServer.classList.remove('active');
    }

    // Activate new selection
    const serverEl = document.querySelector(`.server-item[data-id="${serverId}"]`);
    if (serverEl) {
        serverEl.classList.add('active');
    }

    // Update state
    currentState.selectedServerId = serverId;
    currentState.selectedChannelId = null;
    currentState.isDmView = false;

    // Update UI
    guildNameElement.textContent = currentState.servers.find(s => s.id === serverId)?.name || 'Server';

    // Fetch channels and members
    fetchGuildChannels(serverId);
    fetchGuildMembers(serverId);

    // Show user list
    usersList.style.display = 'block';
}

function selectChannel(channelId) {
    // Deactivate previous selection
    const prevSelectedChannel = document.querySelector('.channel-item.active');
    if (prevSelectedChannel) {
        prevSelectedChannel.classList.remove('active');
    }

    // Activate new selection
    const channelEl = document.querySelector(`.channel-item[data-id="${channelId}"]`);
    if (channelEl) {
        channelEl.classList.add('active');
    }

    // Update state
    currentState.selectedChannelId = channelId;

    // Update UI
    const channel = currentState.channels.find(c => c.id === channelId);
    if (channel) {
        channelInfoElement.innerHTML = `
            <span class="channel-icon">${channel.type === 'TEXT' ? '#' : '<i class="fas fa-volume-up"></i>'}</span>
            <span class="channel-name">${channel.name}</span>
        `;
    }

    // Fetch messages
    fetchMessages(channelId);

    // Subscribe to channel events
    sendWebSocketMessage('SUBSCRIBE_CHANNEL', { channelId });
}

function selectDmUser(userId) {
    // Deactivate previous selection
    const prevSelectedDm = document.querySelector('.dm-item.active');
    if (prevSelectedDm) {
        prevSelectedDm.classList.remove('active');
    }

    // Activate new selection
    const dmEl = document.querySelector(`.dm-item[data-id="${userId}"]`);
    if (dmEl) {
        dmEl.classList.add('active');
    }

    // Update state
    currentState.selectedDmUserId = userId;
    currentState.isDmView = true;

    // Update UI
    const user = currentState.dmUsers.find(u => u.id === userId);
    if (user) {
        channelInfoElement.innerHTML = `
            <div class="user-avatar">
                ${user.avatarUrl ? `<img src="${user.avatarUrl}" alt="${user.username}">` : user.username.charAt(0)}
            </div>
            <span class="channel-name">${user.username}</span>
        `;
    }

    // Fetch DM messages
    fetchDmMessages(userId);

    // Subscribe to DM events
    sendWebSocketMessage('SUBSCRIBE_DM', { userId });

    // Hide user list
    usersList.style.display = 'none';
}

function selectUser(userId) {
    // Start a DM with this user
    selectDmUser(userId);
}

function switchToDmView() {
    // Update state
    currentState.isDmView = true;
    currentState.selectedServerId = null;
    currentState.selectedChannelId = null;

    // Update UI
    guildNameElement.textContent = 'Direct Messages';
    channelInfoElement.innerHTML = `
        <span class="channel-name">Home</span>
    `;

    // Deactivate selected server
    const selectedServer = document.querySelector('.server-item.active');
    if (selectedServer) {
        selectedServer.classList.remove('active');
    }

    // Activate Home button
    const homeButton = document.querySelector('.server-item.home');
    if (homeButton) {
        homeButton.classList.add('active');
    }

    // Fetch DMs
    fetchDMs();

    // Hide user list
    usersList.style.display = 'none';

    // Clear messages
    currentState.messages = [];
    messagesContainer.innerHTML = `
        <div class="welcome-message">
            <h2>Welcome to Direct Messages</h2>
            <p>Select a user to start chatting!</p>
        </div>
    `;
}

// Event Listeners
messageForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    const content = messageInput.value.trim();
    if (!content) return;

    if (currentState.isDmView && currentState.selectedDmUserId) {
        // Send DM
        const response = await fetch(`${getApiBaseUrl}/users/${currentState.selectedDmUserId}/dm`, {
            method: 'POST',
        });

        if (response.ok) {
            const dmChannel = await response.json();
            await sendMessage(dmChannel.id, content);
        }
    } else if (!currentState.isDmView && currentState.selectedChannelId) {
        // Send message to channel
        await sendMessage(currentState.selectedChannelId, content);
    }

    // Clear input
    messageInput.value = '';
});

// Add event listener for Home button
document.querySelector('.server-item.home').addEventListener('click', () => {
    switchToDmView();
});

// Add event listener for DM button
document.querySelector('.server-item.add-server').addEventListener('click', () => {
    switchToDmView();
});

// Initialize
async function init() {
    // Fetch bot info
    await fetchBotUser();

    // Fetch servers
    await fetchGuilds();

    // Start in DM view
    switchToDmView();

    // Initialize WebSocket
    initWebSocket();
}

// Start the application
document.addEventListener('DOMContentLoaded', init);*/

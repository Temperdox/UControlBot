// Initialize app with loading screen
window.app = window.app || {};
window.app.init = async function() {
    // Show loading screen
    const loadingScreen = window.ui.showLoadingScreen();

    try {
        // Initialize UI
        window.ui.init();

        try {
            // Fetch bot info
            await window.api.fetchBotUser();
            window.ui.updateBotInfo();
        } catch (e) {
            console.error("Could not fetch bot user, continuing...", e);
        }

        try {
            // Fetch servers
            await window.api.fetchGuilds();
            window.ui.renderServers();
        } catch (e) {
            console.error("Could not fetch guilds, continuing...", e);
        }

        // Start in DM view
        window.ui.switchToDmView();

        // Initialize WebSocket
        window.ui.initWebSocket();

        // Hide loading screen
        window.ui.hideLoadingScreen(loadingScreen);
    } catch (error) {
        console.error("Error during app initialization:", error);
        window.ui.showLoadingError(loadingScreen, error);
    }
};

// Start the application when DOM is loaded
document.addEventListener('DOMContentLoaded', window.app.init);// Render the channel list
window.ui.renderChannels = function() {
    channelsList.innerHTML = '';

    // Group channels by category
    const categories = {};
    const uncategorizedChannels = [];

    window.currentState.channels.forEach(channel => {
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
            <div class="category-header">
                <span class="category-arrow">▼</span>
                <span class="category-name">${category.name}</span>
            </div>
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
        const categoryHeader = categoryEl.querySelector('.category-header');
        categoryHeader.addEventListener('click', () => {
            categoryEl.classList.toggle('collapsed');
            channelsContainerEl.style.display =
                categoryEl.classList.contains('collapsed') ? 'none' : 'block';

            // Update arrow
            const arrow = categoryEl.querySelector('.category-arrow');
            if (arrow) {
                arrow.textContent = categoryEl.classList.contains('collapsed') ? '▶' : '▼';
            }
        });
    });
};

function createChannelElement(channel) {
    const channelEl = document.createElement('div');
    channelEl.className = `channel-item ${channel.type.toLowerCase()}${
        window.currentState.selectedChannelId === channel.id ? ' active' : ''
    }`;
    channelEl.dataset.id = channel.id;

    // Icon based on channel type
    let icon = '';
    if (channel.type === 'TEXT') {
        icon = `<span class="channel-icon">#</span>`;
    } else if (channel.type === 'VOICE') {
        icon = `<span class="channel-icon"><i class="fas fa-volume-up"></i></span>`;
    } else if (channel.type === 'FORUM') {
        icon = `<span class="channel-icon"><i class="fas fa-comments"></i></span>`;
    } else if (channel.type === 'NEWS') {
        icon = `<span class="channel-icon"><i class="fas fa-newspaper"></i></span>`;
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
    window.currentState.selectedServerId = serverId;
    window.currentState.selectedChannelId = null;
    window.currentState.isDmView = false;

    // Update UI
    const server = window.currentState.servers.find(s => s.id === serverId);
    if (server) {
        guildNameElement.textContent = server.name;
    }

    // Clear current user list while loading
    window.currentState.users = [];
    window.ui.renderUsers();

    // Show loading indicator for users
    usersList.innerHTML = '<div class="loading-indicator"><div class="spinner"></div><div>Loading members...</div></div>';

    // Fetch channels and members
    window.api.fetchGuildChannels(serverId)
        .then(() => {
            // Find the first text channel and select it
            const firstTextChannel = window.currentState.channels.find(
                channel => channel.type === 'TEXT'
            );

            if (firstTextChannel) {
                selectChannel(firstTextChannel.id);
            }
        });

    // Fetch members with a clear loading state
    window.api.fetchGuildMembers(serverId)
        .then(members => {
            console.log(`Loaded ${members.length} members for server ${serverId}`);
            // Show user list
            usersList.style.display = 'block';
        })
        .catch(error => {
            usersList.innerHTML = '<div class="error-message">Failed to load members. Please try again.</div>';
            console.error('Error loading members:', error);
        });

    // Subscribe to guild events
    if (window.currentState.webSocket && window.currentState.webSocket.readyState === WebSocket.OPEN) {
        window.currentState.webSocket.send(JSON.stringify({
            type: 'SUBSCRIBE_GUILD',
            data: { guildId: serverId }
        }));
    }
}

function selectChannel(channelId) {
    console.log(`selectChannel called with ID: ${channelId}`);

    // Rest of your existing code...

    // Right before fetching messages
    console.log(`About to fetch messages for channel ID: ${channelId}`);
    console.log(`Current state:`, {
        selectedChannelId: window.currentState.selectedChannelId,
        isDmView: window.currentState.isDmView
    });

    // Fetch messages
    window.api.fetchMessages(channelId);
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
    window.currentState.selectedChannelId = channelId;

    // Update UI
    const channel = window.currentState.channels.find(c => c.id === channelId);
    if (channel) {
        channelInfoElement.innerHTML = `
            <span class="channel-icon">${channel.type === 'TEXT' ? '#' : '<i class="fas fa-volume-up"></i>'}</span>
            <span class="channel-name">${channel.name}</span>
        `;

        if (channel.topic) {
            channelInfoElement.innerHTML += `
                <div class="channel-topic" title="${channel.topic}">
                    ${channel.topic}
                </div>
            `;
        }
    }

    // Fetch messages
    window.api.fetchMessages(channelId);

    // Enable input
    if (messageInput) {
        messageInput.disabled = false;
        messageInput.placeholder = 'Send a message...';
        messageInput.focus();
    }

    // Subscribe to channel events
    if (window.currentState.webSocket && window.currentState.webSocket.readyState === WebSocket.OPEN) {
        window.currentState.webSocket.send(JSON.stringify({
            type: 'SUBSCRIBE_CHANNEL',
            data: { channelId }
        }));
    }
}

// Render the user list
window.ui.renderUsers = function() {
    usersList.innerHTML = '';

    // Group users by status
    const groups = {
        online: [],
        idle: [],
        dnd: [],
        offline: []
    };

    window.currentState.users.forEach(user => {
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
                const avatarText = (user.displayName || user.username || 'U').charAt(0).toUpperCase();
                avatar = `<div class="avatar-text">${avatarText}</div>`;
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
};

// Render the DM list
window.ui.renderDmList = function() {
    channelsList.innerHTML = '';

    window.currentState.dmUsers.forEach(user => {
        const dmEl = document.createElement('div');
        dmEl.className = `dm-item${window.currentState.selectedDmUserId === user.id ? ' active' : ''}`;

        // Add special classes
        if (user.isOwner) {
            dmEl.classList.add('owner');
            console.log("Rendering owner:", user);
        }

        // Bot detection - JDA sends this as 'bot' not 'isBot'
        /*const isBot = user.bot === true || user.isBot === true;*/
        const isBot = user['isBot'] === true;
        if (isBot) {
            dmEl.classList.add('bot');
        }

        dmEl.dataset.id = user.id;

        let avatar = '';
        if (user.avatarUrl) {
            avatar = `<img src="${user.avatarUrl}" alt="${user.username}">`;
        } else {
            // Use first letter of display name as avatar
            const displayName = user.displayName || user.globalName || user.username;
            const avatarText = displayName.charAt(0).toUpperCase();
            avatar = `<div class="avatar-text">${avatarText}</div>`;
        }

        // Get status color
        const statusClass = user.status ? user.status.toLowerCase() : 'offline';
        console.log(`User ${user.username} status: ${statusClass}`);

        // Use displayName if available, fall back to globalName, then username
        const displayName = user.displayName || user.globalName || user.username;

        // Add badges
        let badges = '';

        // Owner badge (crown icon)
        if (user.isOwner) {
            badges += ' <span class="owner-badge"><i class="fas fa-crown"></i></span>';
        }

        // Bot badge (robot icon)
        if (isBot) {
            badges += ' <span class="bot-badge"><i class="fas fa-robot"></i></span>';
        }

        dmEl.innerHTML = `
            <div class="dm-avatar">
                ${avatar}
                <div class="dm-status ${statusClass}"></div>
            </div>
            <span class="dm-name">${displayName}${badges}</span>
        `;

        dmEl.addEventListener('click', () => {
            selectDmUser(user.id);
        });

        channelsList.appendChild(dmEl);
    });
};

// Update the bot info in the status bar
window.ui.updateBotInfo = function() {
    if (!window.currentState.botUser) return;

    const botName = document.querySelector('.bot-name');
    const botDiscriminator = document.querySelector('.bot-discriminator');
    const botAvatar = document.querySelector('.bot-avatar');

    botName.textContent = window.currentState.botUser.username;

    if (window.currentState.botUser.discriminator && window.currentState.botUser.discriminator !== '0') {
        botDiscriminator.textContent = `#${window.currentState.botUser.discriminator}`;
    } else {
        botDiscriminator.textContent = '';
    }

    if (window.currentState.botUser.avatarUrl) {
        botAvatar.innerHTML = `<img src="${window.currentState.botUser.avatarUrl}" alt="${window.currentState.botUser.username}">`;

        // Update favicon to use bot's avatar
        const faviconLinks = document.querySelectorAll('link[rel="icon"], link[rel="shortcut icon"]');
        faviconLinks.forEach(link => {
            link.href = `/favicon.ico?t=${Date.now()}`; // Add timestamp to force refresh
        });
    } else {
        const avatarText = (window.currentState.botUser.username || 'B').charAt(0).toUpperCase();
        botAvatar.innerHTML = `<div class="default-avatar">${avatarText}</div>`;
    }
};

function selectDmUser(userId) {
    const user = window.currentState.dmUsers.find(u => u.id === userId);

    // Check if user is a bot
    if (user && user['isBot'] === true) {
        // Show toast message
        showToast('You cannot message bots directly', 'error');

        // Highlight the selected user in the DM list but don't open a conversation
        const prevSelectedDm = document.querySelector('.dm-item.active');
        if (prevSelectedDm) {
            prevSelectedDm.classList.remove('active');
        }

        const dmEl = document.querySelector(`.dm-item[data-id="${userId}"]`);
        if (dmEl) {
            dmEl.classList.add('active');
        }

        // Update state partially - set selectedDmUserId but don't change view
        window.currentState.selectedDmUserId = userId;

        return; // Exit early to prevent switching to message view
    }

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
    window.currentState.selectedDmUserId = userId;
    window.currentState.isDmView = true;
    window.currentState.selectedChannelId = null;

    // Update UI
    if (user) {
        let avatar = '';
        if (user.avatarUrl) {
            avatar = `<img src="${user.avatarUrl}" alt="${user.username}">`;
        } else {
            // Use first letter of display name as avatar
            const displayName = user.displayName || user.globalName || user.username;
            const avatarText = displayName.charAt(0).toUpperCase();
            avatar = `<div class="avatar-text">${avatarText}</div>`;
        }

        // Use displayName if available
        const displayName = user.displayName || user.globalName || user.username;

        channelInfoElement.innerHTML = `
            <div class="user-avatar">
                ${avatar}
            </div>
            <span class="channel-name">${displayName}</span>
        `;
    }

    // Show loading state
    window.ui.showChannelLoading();

    // Fetch DM messages
    console.log("Fetching DM messages for user:", userId);
    window.api.fetchDmMessages(userId);

    // Enable input
    if (messageInput) {
        messageInput.disabled = false;
        messageInput.placeholder = `Message @${user?.username || 'User'}`;
        messageInput.focus();
    }

    // Subscribe to DM events
    if (window.currentState.webSocket && window.currentState.webSocket.readyState === WebSocket.OPEN) {
        window.currentState.webSocket.send(JSON.stringify({
            type: 'SUBSCRIBE_DM',
            data: { userId }
        }));
    }

    // Hide user list
    usersList.style.display = 'none';
}

// Handle context menu actions
function handleContextMenuAction(action, messageId, message) {
    switch (action) {
        case 'reply':
            replyToMessage(messageId, message);
            break;

        case 'edit':
            editMessage(messageId, message);
            break;

        case 'delete':
            deleteMessage(messageId);
            break;

        case 'copy-text':
            copyMessageText(message.content);
            break;

        case 'copy-id':
            copyToClipboard(messageId);
            break;
    }
}

// Reply to a message
function replyToMessage(messageId, message) {
    // Create or update reply bar
    let replyBar = document.querySelector('.reply-bar');

    if (!replyBar) {
        replyBar = document.createElement('div');
        replyBar.className = 'reply-bar';
        messageForm.parentNode.insertBefore(replyBar, messageForm);
    }

    const authorName = message.author?.username || 'Unknown User';

    replyBar.innerHTML = `
        <div class="reply-info">
            <span class="reply-icon"><i class="fas fa-reply"></i></span>
            <span>Replying to <strong>${authorName}</strong></span>
            <div class="reply-preview">${message.content.substring(0, 50)}${message.content.length > 50 ? '...' : ''}</div>
        </div>
        <button class="cancel-reply"><i class="fas fa-times"></i></button>
    `;

    // Set state for sending the reply later
    window.currentState.replyingTo = messageId;

    // Add cancel handler
    replyBar.querySelector('.cancel-reply').addEventListener('click', () => {
        replyBar.remove();
        window.currentState.replyingTo = null;
    });

    // Focus the input
    messageInput.focus();
}

// Edit a message
function editMessage(messageId, message) {
    // Find the message element
    const messageEl = document.querySelector(`.message-item[data-id="${messageId}"]`);
    if (!messageEl) return;

    // Get the message text element
    const textEl = messageEl.querySelector('.message-text');
    if (!textEl) return;

    // Replace with an editable input
    const originalContent = message.content;
    textEl.innerHTML = `
        <div class="edit-container">
            <textarea class="edit-input">${originalContent}</textarea>
            <div class="edit-actions">
                <button class="cancel-edit">Cancel</button>
                <button class="save-edit">Save</button>
            </div>
            <div class="edit-hint">escape to <span class="edit-action">cancel</span> • enter to <span class="edit-action">save</span></div>
        </div>
    `;

    // Focus the input
    const editInput = textEl.querySelector('.edit-input');
    editInput.focus();
    editInput.setSelectionRange(originalContent.length, originalContent.length);

    // Add event listeners
    const cancelEdit = () => {
        textEl.innerHTML = formatMessageContent(originalContent);
    };

    const saveEdit = async () => {
        const newContent = editInput.value.trim();
        if (!newContent || newContent === originalContent) {
            cancelEdit();
            return;
        }

        // Show loading state
        textEl.innerHTML = `<div class="loading-edit">Saving edit...</div>`;

        try {
            // API call to edit the message
            await window.api.editMessage(window.currentState.selectedChannelId, messageId, newContent);

            // Update message in state
            const messageIndex = window.currentState.messages.findIndex(msg => msg.id === messageId);
            if (messageIndex !== -1) {
                window.currentState.messages[messageIndex].content = newContent;
                window.currentState.messages[messageIndex].edited = true;
            }

            // Re-render messages
            window.ui.renderMessages();
        } catch (error) {
            console.error('Error editing message:', error);
            textEl.innerHTML = formatMessageContent(originalContent);

            // Show error toast
            showToast('Failed to edit message', 'error');
        }
    };

    // Add button listeners
    textEl.querySelector('.cancel-edit').addEventListener('click', cancelEdit);
    textEl.querySelector('.save-edit').addEventListener('click', saveEdit);

    // Add keyboard listeners
    editInput.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            e.preventDefault();
            cancelEdit();
        } else if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            saveEdit();
        }
    });
}

// Delete a message
function deleteMessage(messageId) {
    // Show confirmation dialog
    if (!confirm('Are you sure you want to delete this message? This cannot be undone.')) {
        return;
    }

    // Call API to delete message
    window.api.deleteMessage(window.currentState.selectedChannelId, messageId)
        .then(() => {
            // Remove message from state
            window.currentState.messages = window.currentState.messages.filter(msg => msg.id !== messageId);

            // Re-render messages
            window.ui.renderMessages();

            // Show success toast
            showToast('Message deleted', 'success');
        })
        .catch(error => {
            console.error('Error deleting message:', error);
            showToast('Failed to delete message', 'error');
        });
}

// Copy text to clipboard
function copyMessageText(text) {
    copyToClipboard(text);
    showToast('Message content copied to clipboard', 'success');
}

// Generic clipboard function
function copyToClipboard(text) {
    // Create temporary textarea
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.style.position = 'fixed';
    textarea.style.opacity = 0;
    document.body.appendChild(textarea);

    // Select and copy
    textarea.select();
    document.execCommand('copy');

    // Clean up
    document.body.removeChild(textarea);
}

// Show toast notification
function showToast(message, type = 'info') {
    // Remove existing toasts
    const existingToasts = document.querySelectorAll('.toast');
    existingToasts.forEach(toast => {
        toast.remove();
    });

    // Create toast
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `
        <div class="toast-content">
            <i class="fas ${type === 'success' ? 'fa-check-circle' : type === 'error' ? 'fa-exclamation-circle' : 'fa-info-circle'}"></i>
            <span>${message}</span>
        </div>
    `;

    // Add to document
    document.body.appendChild(toast);

    // Animate in
    setTimeout(() => {
        toast.classList.add('show');
    }, 10);

    // Animate out and remove
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => {
            if (toast.parentNode) {
                toast.parentNode.removeChild(toast);
            }
        }, 300);
    }, 3000);
}

// Render the server list
window.ui.renderServers = function() {
    serversList.innerHTML = '';

    window.currentState.servers.forEach(server => {
        const serverEl = document.createElement('div');
        serverEl.className = `server-item${window.currentState.selectedServerId === server.id ? ' active' : ''}`;
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
                .substring(0, 2)
                .toUpperCase();
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
};// Generate a consistent color for a user based on their ID
function getAuthorColor(userId) {
    // Discord-like colors for usernames
    const colors = [
        '#1abc9c', '#2ecc71', '#3498db', '#9b59b6', '#e91e63',
        '#f1c40f', '#e67e22', '#e74c3c', '#6e48bb', '#607d8b'
    ];

    // Hash the userId to get a consistent index
    let hash = 0;
    for (let i = 0; i < userId.length; i++) {
        hash = ((hash << 5) - hash) + userId.charCodeAt(i);
        hash |= 0; // Convert to 32bit integer
    }

    // Use absolute value and modulo to get index
    const colorIndex = Math.abs(hash) % colors.length;
    return colors[colorIndex];
}

// Context menu for messages
function showMessageContextMenu(event, message) {
    // Remove any existing context menus
    const existingMenu = document.querySelector('.context-menu');
    if (existingMenu) {
        existingMenu.remove();
    }

    // Create context menu
    const contextMenu = document.createElement('div');
    contextMenu.className = 'context-menu';
    contextMenu.style.top = `${event.clientY}px`;
    contextMenu.style.left = `${event.clientX}px`;

    // Add menu items based on message state
    const isBotMessage = message.author?.id === window.currentState.botUser?.id;

    // Reply option for all messages
    contextMenu.innerHTML = `
        <div class="menu-item" data-action="reply" data-message-id="${message.id}">
            <i class="fas fa-reply"></i> Reply
        </div>
    `;

    // Edit and delete options only for bot's messages
    if (isBotMessage) {
        contextMenu.innerHTML += `
            <div class="menu-item" data-action="edit" data-message-id="${message.id}">
                <i class="fas fa-edit"></i> Edit
            </div>
            <div class="menu-item" data-action="delete" data-message-id="${message.id}">
                <i class="fas fa-trash"></i> Delete
            </div>
        `;
    }

    // Copy options for all messages
    contextMenu.innerHTML += `
        <div class="menu-item" data-action="copy-text" data-message-id="${message.id}">
            <i class="fas fa-copy"></i> Copy Text
        </div>
        <div class="menu-item" data-action="copy-id" data-message-id="${message.id}">
            <i class="fas fa-id-badge"></i> Copy ID
        </div>
    `;

    // Add context menu to body
    document.body.appendChild(contextMenu);

    // Position menu to ensure it stays within window bounds
    const menuRect = contextMenu.getBoundingClientRect();
    if (menuRect.right > window.innerWidth) {
        contextMenu.style.left = `${window.innerWidth - menuRect.width - 10}px`;
    }

    if (menuRect.bottom > window.innerHeight) {
        contextMenu.style.top = `${window.innerHeight - menuRect.height - 10}px`;
    }

    // Add event listeners to menu items
    contextMenu.querySelectorAll('.menu-item').forEach(item => {
        item.addEventListener('click', () => {
            const action = item.dataset.action;
            const messageId = item.dataset.messageId;

            handleContextMenuAction(action, messageId, message);
            contextMenu.remove();
        });
    });

    // Close menu when clicking outside
    document.addEventListener('click', function closeMenu(e) {
        if (!contextMenu.contains(e.target)) {
            contextMenu.remove();
            document.removeEventListener('click', closeMenu);
        }
    });

    // Close menu when scrolling message container
    messagesContainer.addEventListener('scroll', function scrollClose() {
        contextMenu.remove();
        messagesContainer.removeEventListener('scroll', scrollClose);
    });
}// Helper function to format message content with markdown-like parsing
function formatMessageContent(content) {
    if (!content) return '';

    // Escape HTML to prevent XSS
    let formatted = content
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');

    // Format code blocks
    formatted = formatted.replace(/```(\w+)?\n([\s\S]*?)\n```/g, (match, language, code) => {
        const lang = language ? ` class="language-${language}"` : '';
        return `<pre><code${lang}>${code}</code></pre>`;
    });

    // Format inline code
    formatted = formatted.replace(/`([^`]+)`/g, '<code>$1</code>');

    // Format bold text
    formatted = formatted.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');

    // Format italic text
    formatted = formatted.replace(/\*(.+?)\*/g, '<em>$1</em>');

    // Format underline text
    formatted = formatted.replace(/\_\_(.+?)\_\_/g, '<u>$1</u>');

    // Format strikethrough text
    formatted = formatted.replace(/\~\~(.+?)\~\~/g, '<s>$1</s>');

    // Format mentions
    formatted = formatted.replace(/@(\w+)/g, '<span class="mention">@$1</span>');

    // Format channel mentions
    formatted = formatted.replace(/#(\w+)/g, '<span class="channel-mention">#$1</span>');

    // Convert URLs to links
    formatted = formatted.replace(/(https?:\/\/[^\s]+)/g, '<a href="$1" target="_blank" rel="noopener noreferrer">$1</a>');

    // Replace newlines with <br>
    formatted = formatted.replace(/\n/g, '<br>');

    return formatted;
}

// Helper function to render message attachments
function renderMessageAttachments(attachments) {
    if (!attachments || attachments.length === 0) return '';

    let html = '<div class="message-attachments">';

    attachments.forEach(attachment => {
        // Check if it's an image
        const isImage = attachment.contentType && attachment.contentType.startsWith('image/');

        if (isImage) {
            html += `
                <div class="attachment">
                    <img src="${attachment.url}" alt="${attachment.filename}" />
                </div>
            `;
        } else {
            // It's a file
            let fileIcon = 'fa-file';

            // Determine file icon based on contentType
            if (attachment.contentType) {
                if (attachment.contentType.includes('pdf')) fileIcon = 'fa-file-pdf';
                else if (attachment.contentType.includes('word')) fileIcon = 'fa-file-word';
                else if (attachment.contentType.includes('excel') || attachment.contentType.includes('spreadsheet')) fileIcon = 'fa-file-excel';
                else if (attachment.contentType.includes('zip') || attachment.contentType.includes('rar')) fileIcon = 'fa-file-archive';
                else if (attachment.contentType.includes('audio')) fileIcon = 'fa-file-audio';
                else if (attachment.contentType.includes('video')) fileIcon = 'fa-file-video';
                else if (attachment.contentType.includes('text')) fileIcon = 'fa-file-alt';
                else if (attachment.contentType.includes('code') || attachment.filename.endsWith('.js') || attachment.filename.endsWith('.py')) fileIcon = 'fa-file-code';
            }

            // Format file size
            let fileSize = '';
            if (attachment.size) {
                if (attachment.size < 1024) fileSize = `${attachment.size} B`;
                else if (attachment.size < 1024 * 1024) fileSize = `${Math.round(attachment.size / 1024 * 10) / 10} KB`;
                else fileSize = `${Math.round(attachment.size / (1024 * 1024) * 10) / 10} MB`;
            }

            html += `
                <div class="file-attachment">
                    <div class="file-icon">
                        <i class="fas ${fileIcon}"></i>
                    </div>
                    <div class="file-info">
                        <div class="file-name">${attachment.filename}</div>
                        <div class="file-size">${fileSize}</div>
                    </div>
                    <a href="${attachment.url}" target="_blank" rel="noopener noreferrer" class="download-button">
                        <i class="fas fa-download"></i>
                    </a>
                </div>
            `;
        }
    });

    html += '</div>';
    return html;
}

// Helper function to render message embeds
function renderMessageEmbeds(embeds) {
    if (!embeds || embeds.length === 0) return '';

    let html = '<div class="message-embeds">';

    embeds.forEach(embed => {
        // Determine border color
        const borderColor = embed.color ? `#${embed.color.toString(16).padStart(6, '0')}` : '#4f545c';

        html += `<div class="message-embed" style="border-left-color: ${borderColor}">`;

        // Author section
        if (embed.author) {
            html += `
                <div class="embed-author">
                    ${embed.author.iconUrl ? `<img src="${embed.author.iconUrl}" alt="" />` : ''}
                    ${embed.author.url ? `<a href="${embed.author.url}" target="_blank" rel="noopener noreferrer">${embed.author.name}</a>` : embed.author.name}
                </div>
            `;
        }

        // Title
        if (embed.title) {
            html += `
                <div class="embed-title">
                    ${embed.url ? `<a href="${embed.url}" target="_blank" rel="noopener noreferrer">${embed.title}</a>` : embed.title}
                </div>
            `;
        }

        // Description
        if (embed.description) {
            html += `<div class="embed-description">${formatMessageContent(embed.description)}</div>`;
        }

        // Fields
        if (embed.fields && embed.fields.length > 0) {
            html += '<div class="embed-fields">';

            embed.fields.forEach(field => {
                html += `
                    <div class="embed-field ${field.inline ? 'inline' : ''}">
                        <div class="field-name">${field.name}</div>
                        <div class="field-value">${formatMessageContent(field.value)}</div>
                    </div>
                `;
            });

            html += '</div>';
        }

        // Image
        if (embed.image) {
            html += `
                <div class="embed-image">
                    <img src="${embed.image.url}" alt="" />
                </div>
            `;
        }

        // Thumbnail
        if (embed.thumbnail) {
            html += `
                <div class="embed-thumbnail">
                    <img src="${embed.thumbnail.url}" alt="" />
                </div>
            `;
        }

        // Footer
        if (embed.footer) {
            html += `
                <div class="embed-footer">
                    ${embed.footer.iconUrl ? `<img src="${embed.footer.iconUrl}" alt="" />` : ''}
                    <span>${embed.footer.text}</span>
                    ${embed.timestamp ? `<span class="embed-timestamp"> • ${new Date(embed.timestamp).toLocaleString()}</span>` : ''}
                </div>
            `;
        }

        html += '</div>';
    });

    html += '</div>';
    return html;
}// ui.js - Complete Discord-like UI implementation

// Initialize UI namespace
window.ui = window.ui || {};

// DOM Elements - initialize when DOM is loaded
let serversList, channelsList, messagesContainer, usersList, messageForm,
    messageInput, guildNameElement, channelInfoElement,
    connectionStatusElement, statusIndicator;

// Helper functions
function sendTypingIndicator() {
    if (!window.currentState.webSocket || window.currentState.webSocket.readyState !== WebSocket.OPEN) return;

    try {
        const payload = {
            type: 'TYPING',
            data: {
                channelId: window.currentState.selectedChannelId || null,
                userId: window.currentState.selectedDmUserId || null
            }
        };
        window.currentState.webSocket.send(JSON.stringify(payload));
    } catch (error) {
        console.error('Error sending typing indicator:', error);
    }
}

function handleMessagesScroll() {
    // Implement infinite scrolling for messages
    if (messagesContainer.scrollTop === 0 && window.currentState.messages.length >= 50) {
        // Load more messages when scrolled to top
        const oldestMessageId = window.currentState.messages[0]?.id;
        if (oldestMessageId) {
            loadMoreMessages(oldestMessageId);
        }
    }
}

function loadMoreMessages(beforeId) {
    if (window.currentState.isDmView && window.currentState.selectedDmUserId) {
        window.api.fetchDmMessages(window.currentState.selectedDmUserId, { before: beforeId, limit: 50 });
    } else if (window.currentState.selectedChannelId) {
        window.api.fetchMessages(window.currentState.selectedChannelId, { before: beforeId, limit: 50 });
    }
}

function handleKeyboardShortcuts(e) {
    // Alt+Up/Down to navigate channels
    if (e.altKey && e.key === 'ArrowUp') {
        navigateToPreviousChannel();
        e.preventDefault();
    } else if (e.altKey && e.key === 'ArrowDown') {
        navigateToNextChannel();
        e.preventDefault();
    }

    // Ctrl+K to open quick switcher (channel search)
    if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
        focusChannelSearch();
        e.preventDefault();
    }

    // Escape to cancel current action or close modal
    if (e.key === 'Escape') {
        handleEscapeKey();
        e.preventDefault();
    }
}

function navigateToPreviousChannel() {
    const channels = Array.from(document.querySelectorAll('.channel-item, .dm-item'));
    const currentIndex = channels.findIndex(el => el.classList.contains('active'));

    if (currentIndex > 0) {
        const prevChannel = channels[currentIndex - 1];
        const channelId = prevChannel.dataset.id;

        if (prevChannel.classList.contains('dm-item')) {
            selectDmUser(channelId);
        } else {
            selectChannel(channelId);
        }
    }
}

function navigateToNextChannel() {
    const channels = Array.from(document.querySelectorAll('.channel-item, .dm-item'));
    const currentIndex = channels.findIndex(el => el.classList.contains('active'));

    if (currentIndex < channels.length - 1 && currentIndex !== -1) {
        const nextChannel = channels[currentIndex + 1];
        const channelId = nextChannel.dataset.id;

        if (nextChannel.classList.contains('dm-item')) {
            selectDmUser(channelId);
        } else {
            selectChannel(channelId);
        }
    }
}

function focusChannelSearch() {
    const searchInput = document.querySelector('.channels-search input');
    if (searchInput) {
        searchInput.focus();
    }
}

function handleEscapeKey() {
    // Close any open modals or menus
    const openModals = document.querySelectorAll('.modal.active');
    openModals.forEach(modal => {
        modal.classList.remove('active');
    });

    // Clear search if focused
    const searchInput = document.querySelector('.channels-search input');
    if (document.activeElement === searchInput) {
        searchInput.value = '';
        searchInput.blur();
    }
}

// Initialize UI namespace with enhanced functionality
window.ui.init = function() {
    // Initialize DOM references
    serversList = document.getElementById('servers-container');
    channelsList = document.getElementById('channels-list');
    messagesContainer = document.getElementById('messages-container');
    usersList = document.getElementById('users-container');
    messageForm = document.getElementById('message-form');
    messageInput = document.getElementById('message-input');
    guildNameElement = document.getElementById('guild-name');
    channelInfoElement = document.getElementById('channel-info');
    connectionStatusElement = document.getElementById('connection-status');
    statusIndicator = document.querySelector('.status-indicator');

    // Add event listeners
    messageForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const content = messageInput.value.trim();
        if (!content) return;

        // Optimistic UI update - show message immediately
        const optimisticMessage = {
            id: 'temp-' + Date.now(),
            content: content,
            author: window.currentState.botUser,
            timestamp: Date.now(),
            pending: true
        };

        window.currentState.messages.push(optimisticMessage);
        window.ui.renderMessages();

        // Clear input immediately for better UX
        messageInput.value = '';

        try {
            if (window.currentState.isDmView && window.currentState.selectedDmUserId) {
                // Send DM
                const response = await fetch(`${window.API_BASE_URL}/users/${window.currentState.selectedDmUserId}/dm`, {
                    method: 'POST',
                });

                if (response.ok) {
                    const dmChannel = await response.json();
                    await window.api.sendMessage(dmChannel.id, content);
                }
            } else if (!window.currentState.isDmView && window.currentState.selectedChannelId) {
                // Send message to channel
                await window.api.sendMessage(window.currentState.selectedChannelId, content);
            }

            // Remove optimistic message and fetch actual messages
            window.currentState.messages = window.currentState.messages.filter(msg => msg.id !== optimisticMessage.id);

            if (window.currentState.isDmView && window.currentState.selectedDmUserId) {
                await window.api.fetchDmMessages(window.currentState.selectedDmUserId);
            } else if (window.currentState.selectedChannelId) {
                await window.api.fetchMessages(window.currentState.selectedChannelId);
            }
        } catch (error) {
            console.error('Error sending message:', error);

            // Show error state for optimistic message
            const errorMsgIndex = window.currentState.messages.findIndex(msg => msg.id === optimisticMessage.id);
            if (errorMsgIndex !== -1) {
                window.currentState.messages[errorMsgIndex].error = true;
                window.ui.renderMessages();
            }
        }
    });

    // Add event listener for Home button
    document.querySelector('.server-item.home')?.addEventListener('click', () => {
        window.ui.switchToDmView();
    });

    // Add event listener for DM button
    document.querySelector('.server-item.add-server')?.addEventListener('click', () => {
        window.ui.switchToDmView();
    });

    // Enable input field only when appropriate
    messageInput.disabled = !(window.currentState.selectedChannelId || window.currentState.selectedDmUserId);

    // Add typing indicator
    messageInput.addEventListener('input', () => {
        if (messageInput.value.trim() && (window.currentState.selectedChannelId || window.currentState.selectedDmUserId)) {
            // Send typing indicator event
            sendTypingIndicator();
        }
    });

    // Make sure scrolling behavior is smooth
    messagesContainer.addEventListener('scroll', handleMessagesScroll);

    // Add keyboard shortcuts
    document.addEventListener('keydown', handleKeyboardShortcuts);
};

// Initialize WebSocket with improved reconnection strategy
window.ui.initWebSocket = function() {
    const wsUrl = `ws://${window.location.host}/ws`;

    // Close existing connection if any
    if (window.currentState.webSocket &&
        (window.currentState.webSocket.readyState === WebSocket.OPEN ||
            window.currentState.webSocket.readyState === WebSocket.CONNECTING)) {
        console.warn('WebSocket already open or connecting. Skipping init.');
        return;
    }

    const ws = new WebSocket(wsUrl);
    let pingInterval = null;

    ws.onopen = () => {
        console.log('WebSocket connected');
        window.currentState.isConnected = true;
        window.ui.updateConnectionStatus();

        // Start ping interval to keep connection alive
        pingInterval = setInterval(() => {
            if (ws.readyState === WebSocket.OPEN) {
                ws.send(JSON.stringify({
                    type: 'PING',
                    timestamp: Date.now()
                }));
                console.log('Ping sent to keep connection alive');
            }
        }, 15000); // Send ping every 15 seconds

        // Send identity upon connection
        if (window.currentState.botUser) {
            ws.send(JSON.stringify({
                type: 'IDENTIFY',
                data: {
                    botId: window.currentState.botUser.id || '0'
                }
            }));
            console.log('Sent IDENTIFY message');
        }
    };

    ws.onmessage = (event) => {
        try {
            const message = JSON.parse(event.data);
            console.log('WebSocket message received:', message);

            // Handle welcome message
            if (message.type === 'WELCOME') {
                console.log('Received welcome message:', message.data.message);

                // Subscribe to current view after welcome
                if (window.currentState.isDmView && window.currentState.selectedDmUserId) {
                    ws.send(JSON.stringify({
                        type: 'SUBSCRIBE_DM',
                        data: {
                            userId: window.currentState.selectedDmUserId
                        }
                    }));
                    console.log('Sent SUBSCRIBE_DM for user:', window.currentState.selectedDmUserId);
                } else if (window.currentState.selectedChannelId) {
                    ws.send(JSON.stringify({
                        type: 'SUBSCRIBE_CHANNEL',
                        data: {
                            channelId: window.currentState.selectedChannelId
                        }
                    }));
                    console.log('Sent SUBSCRIBE_CHANNEL for channel:', window.currentState.selectedChannelId);
                }
            }

            // Handle different event types
            window.ui.handleWebSocketEvent(message);
        } catch (error) {
            console.error('Error parsing WebSocket message:', error);
        }
    };

    ws.onclose = (event) => {
        console.log(`WebSocket disconnected with code ${event.code}`);
        window.currentState.isConnected = false;
        window.ui.updateConnectionStatus();

        // Clear ping interval
        if (pingInterval) {
            clearInterval(pingInterval);
        }

        // Implement exponential backoff for reconnection
        const delay = Math.min(2000, 30000);
        console.log(`Attempting to reconnect in ${delay/1000} seconds...`);

        if (event.code !== 1000) { // 1000 means "Normal Closure"
            const delay = Math.min(2000, 30000);
            console.log(`Reconnecting in ${delay / 1000}s...`);
            setTimeout(() => {
                window.ui.initWebSocket();
            }, delay);
        } else {
            console.log('WebSocket closed normally. Not reconnecting.');
        }
    };

    ws.onerror = (error) => {
        console.error('WebSocket error:', error);
        window.currentState.isConnected = false;
        window.ui.updateConnectionStatus();
    };

    window.currentState.webSocket = ws;
};

window.ui.updateConnectionStatus = function() {
    if (window.currentState.isConnected) {
        connectionStatusElement.textContent = 'Connected';
        statusIndicator.classList.remove('offline');
        statusIndicator.classList.add('online');

        // Add a nice animation effect for connection
        statusIndicator.classList.add('pulse');
        setTimeout(() => {
            statusIndicator.classList.remove('pulse');
        }, 1500);
    } else {
        connectionStatusElement.textContent = 'Disconnected';
        statusIndicator.classList.remove('online');
        statusIndicator.classList.add('offline');
    }
};

function selectUser(userId) {
    // Start a DM with the selected user
    window.api.createDMChannel(userId)
        .then(dmChannel => {
            selectDmUser(userId);
        })
        .catch(error => {
            console.error('Error creating DM channel:', error);
            showToast('Failed to start conversation', 'error');
        });
}

// Switch to DM view
window.ui.switchToDmView = function() {
    console.log("Switching to DM view...");

    // Show loading indicator
    const loadingIndicator = document.createElement('div');
    loadingIndicator.className = 'loading-indicator';
    loadingIndicator.innerHTML = '<div class="spinner"></div><div>Loading direct messages...</div>';
    document.querySelector('#channels-list').appendChild(loadingIndicator);

    // Update state immediately
    window.currentState.isDmView = true;
    window.currentState.selectedServerId = null;
    window.currentState.selectedChannelId = null;
    window.currentState.selectedDmUserId = null;

    // Update UI headers
    guildNameElement.textContent = 'Direct Messages';
    channelInfoElement.innerHTML = `
        <span class="channel-name">Home</span>
    `;

    // Deactivate selected server
    const selectedServer = document.querySelector('.server-item.active:not(.home)');
    if (selectedServer) {
        selectedServer.classList.remove('active');
    }

    // Activate Home button
    const homeButton = document.querySelector('.server-item.home');
    if (homeButton) {
        homeButton.classList.add('active');
    }

    // Hide user list
    usersList.style.display = 'none';

    // Clear messages and show welcome screen
    window.currentState.messages = [];
    messagesContainer.innerHTML = `
        <div class="welcome-message">
            <h2>Welcome to Direct Messages</h2>
            <p>Select a user to start chatting!</p>
        </div>
    `;

    // Disable input until a DM is selected
    if (messageInput) {
        messageInput.disabled = true;
        messageInput.placeholder = 'Select a conversation...';
    }

    // Always fetch all users when switching to DM view
    window.api.fetchDMs().then(() => {
        // Remove loading indicator
        const indicator = document.querySelector('.loading-indicator');
        if (indicator) {
            indicator.remove();
        }
        console.log("DM list loaded successfully");
    }).catch(error => {
        console.error("Error loading DM list:", error);
        // Show error in the channel list area
        document.querySelector('#channels-list').innerHTML = `
            <div class="error-message">
                Failed to load direct messages. Please try again.
            </div>
            <button class="retry-button" onclick="window.ui.switchToDmView()">Retry</button>
        `;
    });
};

// Show loading screen for app initialization
window.ui.showLoadingScreen = function() {
    const loadingScreen = document.createElement('div');
    loadingScreen.className = 'loading-screen';
    loadingScreen.innerHTML = `
        <div class="loading-logo">
            <i class="fa-brands fa-discord"></i>
        </div>
        <div class="loading-text">Loading Discord</div>
        <div class="loading-bar">
            <div class="loading-progress"></div>
        </div>
    `;
    document.body.appendChild(loadingScreen);

    return loadingScreen;
};

// Hide loading screen with animation
window.ui.hideLoadingScreen = function(loadingScreen) {
    loadingScreen.style.opacity = '0';
    setTimeout(() => {
        loadingScreen.style.visibility = 'hidden';
        if (loadingScreen.parentNode) {
            document.body.removeChild(loadingScreen);
        }
    }, 300);
};

// Show loading error in loading screen
window.ui.showLoadingError = function(loadingScreen, error) {
    loadingScreen.innerHTML = `
        <div class="loading-logo">
            <i class="fa-brands fa-discord"></i>
        </div>
        <div class="loading-text">Error loading Discord</div>
        <p style="color: var(--text-muted); margin-bottom: 16px;">
            ${error.message || 'Something went wrong. Please refresh the page.'}
        </p>
        <button onclick="location.reload()" style="padding: 8px 16px; background-color: var(--brand-experiment); color: white; border: none; border-radius: 4px; cursor: pointer;">
            Refresh
        </button>
    `;
};

// Channel loading state
window.ui.showChannelLoading = function() {
    if (messagesContainer) {
        messagesContainer.innerHTML = `
            <div class="loading-container">
                <div class="loading-spinner"></div>
                <div class="loading-text">Loading messages...</div>
            </div>
        `;
    }
};

window.ui.handleWebSocketEvent = function(event) {
    const { type, data } = event;

    switch (type) {
        case 'WELCOME':
            console.log(`WebSocket connection established. Session ID: ${data.sessionId}`);
            break;

        case 'ACK':
            console.log(`Action acknowledged: ${data.action} for ID: ${data.id}`);
            break;

        case 'REFRESH_DM_LIST':
            window.api.fetchDMs();
            break;

        case 'PONG':
            // No need to do anything for pong responses
            break;

        case 'MESSAGE_RECEIVED':
            window.ui.handleNewMessage(data);
            break;

        case 'MESSAGE_UPDATE':
            window.ui.handleMessageUpdate(data);
            break;

        case 'MESSAGE_DELETE':
            window.ui.handleMessageDelete(data);
            break;

        case 'USER_UPDATE':
            window.ui.handleUserUpdate(data);
            break;

        case 'USER_UPDATE_STATUS':
            window.ui.handleStatusUpdate(data);
            break;

        case 'PRESENCE_UPDATE':
            window.ui.handlePresenceUpdate(data);
            break;

        case 'TYPING_START':
            window.ui.handleTypingStart(data);
            break;

        case 'GUILD_MEMBER_JOIN':
            window.ui.handleMemberJoin(data);
            break;

        case 'GUILD_MEMBER_LEAVE':
            window.ui.handleMemberLeave(data);
            break;

        case 'CHANNEL_CREATE':
            window.ui.handleChannelCreate(data);
            break;

        case 'CHANNEL_UPDATE':
            window.ui.handleChannelUpdate(data);
            break;

        case 'CHANNEL_DELETE':
            window.ui.handleChannelDelete(data);
            break;

        case 'GUILD_UPDATE':
            window.ui.handleGuildUpdate(data);
            break;

        case 'VOICE_STATE_UPDATE':
            window.ui.handleVoiceStateUpdate(data);
            break;

        default:
            console.log(`Unhandled WebSocket event type: ${type}`);
    }
};

window.ui.handleNewMessage = function(messageData) {
    // Check if this message belongs to the current view
    const isCurrentChannel = window.currentState.selectedChannelId === messageData.channelId;
    const isCurrentDm = window.currentState.isDmView &&
        (messageData.author?.id === window.currentState.selectedDmUserId ||
            messageData.recipientId === window.currentState.selectedDmUserId);

    if (isCurrentChannel || isCurrentDm) {
        // Remove typing indicator if it's from the same user
        const typingIndicator = document.querySelector('.typing-indicator');
        if (typingIndicator && messageData.author) {
            const typingText = typingIndicator.querySelector('.typing-text');
            if (typingText && typingText.textContent.includes(messageData.author.username)) {
                typingIndicator.remove();
            }
        }

        // Add message to the current list and render
        window.currentState.messages.push(messageData);
        window.ui.renderMessages();

        // Scroll to bottom if we're already near the bottom
        const isAtBottom = messagesContainer.scrollHeight - messagesContainer.scrollTop <= messagesContainer.clientHeight + 100;
        if (isAtBottom) {
            messagesContainer.scrollTop = messagesContainer.scrollHeight;
        } else {
            // Show new message indicator
            showNewMessageIndicator();
        }
    }
};

window.ui.handleStatusUpdate = function(data) {
    // Find the user in the DM list
    const userId = data.userId;
    const newStatus = data.newStatus.toLowerCase();
    const isOwner = data.isOwner === true;

    console.log(`Status update for user ${data.userName} (${userId}): ${data.oldStatus} -> ${newStatus} ${isOwner ? "[OWNER]" : ""}`);

    // Update user in DM list with priority for owner
    if (window.currentState.dmUsers) {
        const userIndex = window.currentState.dmUsers.findIndex(user => user.id === userId);
        if (userIndex !== -1) {
            window.currentState.dmUsers[userIndex].status = newStatus;
            // Only re-render if we're in DM view
            if (window.currentState.isDmView) {
                window.ui.renderDmList();
            }
        } else if (isOwner) {
            // If owner not in list but this is owner update, fetch DMs to refresh
            console.log("Owner status update detected but owner not in list - refreshing DM list");
            window.api.fetchDMs();
        }
    }

    // Update user in users list for server view
    if (window.currentState.users) {
        const userIndex = window.currentState.users.findIndex(user => user.id === userId);
        if (userIndex !== -1) {
            window.currentState.users[userIndex].status = newStatus;
            // Only re-render if we're in server view and not DM view
            if (!window.currentState.isDmView) {
                window.ui.renderUsers();
            }
        }
    }
};

function showNewMessageIndicator() {
    // Remove existing indicator if any
    const existingIndicator = document.querySelector('.new-message-indicator');
    if (existingIndicator) {
        existingIndicator.remove();
    }

    // Create new indicator
    const indicator = document.createElement('div');
    indicator.className = 'new-message-indicator';
    indicator.innerHTML = `
        <div class="indicator-content">
            <i class="fas fa-arrow-down"></i>
            <span>New Messages</span>
        </div>
    `;

    // Add to container
    messagesContainer.appendChild(indicator);

    // Add click handler to scroll to bottom
    indicator.addEventListener('click', () => {
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
        indicator.remove();
    });

    // Auto-remove after some time
    setTimeout(() => {
        if (indicator.parentNode) {
            indicator.classList.add('fade-out');
            setTimeout(() => {
                if (indicator.parentNode) {
                    indicator.parentNode.removeChild(indicator);
                }
            }, 300);
        }
    }, 5000);
}

// Event handler implementations
window.ui.handleMessageUpdate = function(data) {
    const messageIndex = window.currentState.messages.findIndex(msg => msg.id === data.id);
    if (messageIndex !== -1) {
        // Update existing message in the array
        window.currentState.messages[messageIndex] = {
            ...window.currentState.messages[messageIndex],
            ...data,
            edited: true
        };
        window.ui.renderMessages();
    }
};

window.ui.handleMessageDelete = function(data) {
    // Find and remove the message
    window.currentState.messages = window.currentState.messages.filter(msg => msg.id !== data.id);
    window.ui.renderMessages();
};

window.ui.handleUserUpdate = function(data) {
    // Update user in all relevant state arrays
    if (window.currentState.users) {
        const userIndex = window.currentState.users.findIndex(user => user.id === data.id);
        if (userIndex !== -1) {
            window.currentState.users[userIndex] = { ...window.currentState.users[userIndex], ...data };
        }
    }

    if (window.currentState.dmUsers) {
        const dmUserIndex = window.currentState.dmUsers.findIndex(user => user.id === data.id);
        if (dmUserIndex !== -1) {
            window.currentState.dmUsers[dmUserIndex] = { ...window.currentState.dmUsers[dmUserIndex], ...data };
        }
    }

    // Re-render relevant UI components
    if (window.currentState.isDmView) {
        window.ui.renderDmList();
    } else {
        window.ui.renderUsers();
    }
};

window.ui.handlePresenceUpdate = function(data) {
    // Update presence (online status) for a user
    const updateUserPresence = (userArray) => {
        const userIndex = userArray.findIndex(user => user.id === data.userId);
        if (userIndex !== -1) {
            userArray[userIndex].status = data.status;
        }
    };

    if (window.currentState.users) {
        updateUserPresence(window.currentState.users);
    }

    if (window.currentState.dmUsers) {
        updateUserPresence(window.currentState.dmUsers);
    }

    // Re-render relevant UI components
    if (window.currentState.isDmView) {
        window.ui.renderDmList();
    } else {
        window.ui.renderUsers();
    }
};

window.ui.handleTypingStart = function(data) {
    // Check if typing indicator is for the current channel/DM
    const isRelevantChannel = window.currentState.selectedChannelId === data.channelId;
    const isRelevantDM = window.currentState.isDmView && window.currentState.selectedDmUserId === data.userId;

    if (isRelevantChannel || isRelevantDM) {
        // Show typing indicator
        showTypingIndicator(data.userId);
    }
};

function showTypingIndicator(userId) {
    let typingIndicator = document.querySelector('.typing-indicator');

    if (!typingIndicator) {
        typingIndicator = document.createElement('div');
        typingIndicator.className = 'typing-indicator';
        messagesContainer.appendChild(typingIndicator);
    }

    // Find username for the typing user
    let username = 'Someone';
    const user = findUserById(userId);
    if (user) {
        username = user.username || user.displayName || 'Someone';
    }

    typingIndicator.innerHTML = `
        <div class="typing-animation">
            <span class="dot"></span>
            <span class="dot"></span>
            <span class="dot"></span>
        </div>
        <span class="typing-text">${username} is typing...</span>
    `;

    // Auto-remove typing indicator after 10 seconds
    if (window.typingTimeout) {
        clearTimeout(window.typingTimeout);
    }

    window.typingTimeout = setTimeout(() => {
        if (typingIndicator && typingIndicator.parentNode) {
            typingIndicator.parentNode.removeChild(typingIndicator);
        }
    }, 10000);
}

function findUserById(userId) {
    // Search for user in both users and dmUsers arrays
    let user = null;

    if (window.currentState.users) {
        user = window.currentState.users.find(u => u.id === userId);
    }

    if (!user && window.currentState.dmUsers) {
        user = window.currentState.dmUsers.find(u => u.id === userId);
    }

    return user;
}

window.ui.handleMemberJoin = function(data) {
    // Only update if we're currently viewing the relevant guild
    if (window.currentState.selectedServerId === data.guildId) {
        // Add the new member to users list
        if (!window.currentState.users.some(user => user.id === data.user.id)) {
            window.currentState.users.push(data.user);
            window.ui.renderUsers();
        }

        // Optionally show a server message
        if (window.currentState.selectedChannelId === data.systemChannelId) {
            const joinMessage = {
                id: 'system-' + Date.now(),
                content: `${data.user.username} joined the server.`,
                timestamp: Date.now(),
                system: true
            };

            window.currentState.messages.push(joinMessage);
            window.ui.renderMessages();
        }
    }
};

window.ui.handleMemberLeave = function(data) {
    // Only update if we're currently viewing the relevant guild
    if (window.currentState.selectedServerId === data.guildId) {
        // Remove user from users list
        window.currentState.users = window.currentState.users.filter(user => user.id !== data.userId);
        window.ui.renderUsers();

        // Optionally show a server message
        if (window.currentState.selectedChannelId === data.systemChannelId) {
            const leaveMessage = {
                id: 'system-' + Date.now(),
                content: `${data.username || 'A user'} left the server.`,
                timestamp: Date.now(),
                system: true
            };

            window.currentState.messages.push(leaveMessage);
            window.ui.renderMessages();
        }
    }
};

window.ui.handleChannelCreate = function(data) {
    // Only update if we're currently viewing the relevant guild
    if (window.currentState.selectedServerId === data.guildId) {
        window.currentState.channels.push(data);
        window.ui.renderChannels();
    }
};

window.ui.handleChannelUpdate = function(data) {
    // Update channel in all states
    const channelIndex = window.currentState.channels.findIndex(channel => channel.id === data.id);
    if (channelIndex !== -1) {
        window.currentState.channels[channelIndex] = { ...window.currentState.channels[channelIndex], ...data };
        window.ui.renderChannels();

        // Update channel info if this is the currently selected channel
        if (window.currentState.selectedChannelId === data.id) {
            updateChannelInfo(data);
        }
    }
};

function updateChannelInfo(channel) {
    if (!channelInfoElement) return;

    // Update channel name and info in the header
    channelInfoElement.innerHTML = `
        <span class="channel-icon">${channel.type === 'TEXT' ? '#' : '<i class="fas fa-volume-up"></i>'}</span>
        <span class="channel-name">${channel.name}</span>
    `;
}

window.ui.handleChannelDelete = function(data) {
    // Remove channel from state
    window.currentState.channels = window.currentState.channels.filter(channel => channel.id !== data.id);
    window.ui.renderChannels();

    // If this was the selected channel, switch to another one
    if (window.currentState.selectedChannelId === data.id) {
        // Find first available text channel in the same guild
        const alternateChannel = window.currentState.channels.find(
            channel => channel.type === 'TEXT' && channel.id !== data.id
        );

        if (alternateChannel) {
            selectChannel(alternateChannel.id);
        } else {
            // Clear channel view
            window.currentState.selectedChannelId = null;
            channelInfoElement.innerHTML = '';
            messagesContainer.innerHTML = `
                <div class="welcome-message">
                    <h2>No channels available</h2>
                    <p>Select a different server or DM to start chatting!</p>
                </div>
            `;
        }
    }
};

window.ui.handleGuildUpdate = function(data) {
    // Update guild in servers list
    const serverIndex = window.currentState.servers.findIndex(server => server.id === data.id);
    if (serverIndex !== -1) {
        window.currentState.servers[serverIndex] = { ...window.currentState.servers[serverIndex], ...data };
        window.ui.renderServers();

        // Update guild name if this is the currently selected guild
        if (window.currentState.selectedServerId === data.id) {
            guildNameElement.textContent = data.name;
        }
    }
};

window.ui.handleVoiceStateUpdate = function(data) {
    // Update UI to show who's in voice channels
    if (window.currentState.selectedServerId === data.guildId) {
        // Re-render channels to show voice participants
        window.ui.renderChannels();
    }
};

window.ui.renderMessages = function() {
    // Save scroll position
    const wasAtBottom = messagesContainer.scrollHeight - messagesContainer.scrollTop <= messagesContainer.clientHeight + 50;
    const oldScrollHeight = messagesContainer.scrollHeight;

    // Clear container but preserve typing indicator if present
    const typingIndicator = messagesContainer.querySelector('.typing-indicator');
    if (typingIndicator) {
        typingIndicator.remove(); // temporarily remove
    }

    messagesContainer.innerHTML = '';

    if (window.currentState.messages.length === 0) {
        messagesContainer.innerHTML = `
            <div class="welcome-message">
                <h2>No messages yet</h2>
                <p>Send a message to start the conversation!</p>
            </div>
        `;
        return;
    }

    // Sort messages by timestamp (oldest to newest)
    window.currentState.messages.sort((a, b) => a.timestamp - b.timestamp);

    // Group messages by author to handle continuations
    let lastAuthorId = null;
    let lastTimestamp = null;
    const timestampThreshold = 5 * 60 * 1000; // 5 minutes in milliseconds

    window.currentState.messages.forEach((message, index) => {
        // Check if this is a continuous message from same author within timeframe
        const timeDiff = lastTimestamp ? message.timestamp - lastTimestamp : Infinity;
        const isContinuation = lastAuthorId === message.author?.id && timeDiff < timestampThreshold;

        // Update tracking variables
        lastAuthorId = message.author?.id;
        lastTimestamp = message.timestamp;

        // Create message element
        const messageEl = document.createElement('div');
        messageEl.className = `message-item${isContinuation ? ' continuation' : ''}`;
        messageEl.dataset.id = message.id;
        if (message.author?.id === window.currentState.botUser?.id) {
            messageEl.classList.add('bot-message');
        } else {
            messageEl.classList.add('user-message');
        }

        // Handle system messages differently
        if (message.system) {
            messageEl.className = 'message-item system-message';
            messageEl.innerHTML = `
                <div class="system-message-content">
                    <div class="system-message-text">${message.content}</div>
                </div>
            `;
            messagesContainer.appendChild(messageEl);
            return; // Skip the rest of the processing for system messages
        }

        // Handle pending or error message states
        if (message.pending) {
            messageEl.classList.add('pending');
        }

        if (message.error) {
            messageEl.classList.add('error');
        }

        // Prepare avatar
        let avatar = '';
        if (message.author?.avatarUrl) {
            avatar = `<img src="${message.author.avatarUrl}" alt="${message.author.username}">`;
        } else if (message.author) {
            // Use first letter of username as avatar
            const avatarText = message.author.username.charAt(0);
            avatar = `<div class="avatar-text">${avatarText}</div>`;
        }

        // Format timestamp
        const messageDate = new Date(message.timestamp);
        const now = new Date();
        let timestamp = '';

        // Today, show time only
        if (messageDate.toDateString() === now.toDateString()) {
            timestamp = messageDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        } else {
            // Different day, show date and time
            timestamp = messageDate.toLocaleDateString([], {
                month: 'short',
                day: 'numeric'
            }) + ' at ' + messageDate.toLocaleTimeString([], {
                hour: '2-digit',
                minute: '2-digit'
            });
        }

        // Different layout for initial vs continuation messages
        let messageHTML = '';
        if (!isContinuation) {
            messageHTML = `
                <div class="message-avatar">
                    ${avatar}
                </div>
                <div class="message-content">
                    <div class="message-author">
                        <span class="author-name" style="color: ${getAuthorColor(message.author?.id || '0')}">
                            ${message.author?.displayName || message.author?.globalName || message.author?.username || 'Unknown User'}
                        </span>
                        <span class="message-timestamp" title="${messageDate.toLocaleString()}">
                            ${timestamp}
                        </span>
                        ${message.edited ? '<span class="edited-indicator">(edited)</span>' : ''}
                    </div>
                    <div class="message-text">
                        ${formatMessageContent(message.content)}
                    </div>
                    ${renderMessageAttachments(message.attachments)}
                    ${renderMessageEmbeds(message.embeds)}
                </div>
            `;
        } else {
            // Continuation message (no avatar, no author header)
            messageHTML = `
                <div class="message-avatar-placeholder"></div>
                <div class="message-content">
                    <div class="hover-timestamp">
                        <span class="message-timestamp" title="${messageDate.toLocaleString()}">
                            ${timestamp}
                        </span>
                        ${message.edited ? '<span class="edited-indicator">(edited)</span>' : ''}
                    </div>
                    <div class="message-text">
                        ${formatMessageContent(message.content)}
                    </div>
                    ${renderMessageAttachments(message.attachments)}
                    ${renderMessageEmbeds(message.embeds)}
                </div>
            `;
        }

        messageEl.innerHTML = messageHTML;

        // Add context menu event listeners
        messageEl.addEventListener('contextmenu', (e) => {
            e.preventDefault();
            showMessageContextMenu(e, message);
        });

        // Add message to container
        messagesContainer.appendChild(messageEl);
    });

    // Add typing indicator back if it existed
    if (typingIndicator) {
        messagesContainer.appendChild(typingIndicator);
    }

    // Maintain scroll position or scroll to bottom if already at bottom
    if (wasAtBottom) {
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    } else {
        // Maintain the same relative scroll position
        messagesContainer.scrollTop = messagesContainer.scrollHeight - oldScrollHeight + messagesContainer.scrollTop;
    }
};
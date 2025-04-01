// Database Frontend Service for Discord Bot UI
// This service handles database interactions for the frontend

// Import our services
import dbService from './database-service';

class DatabaseFrontendService {
    constructor() {
        this.initialized = false;
        this.currentState = window.currentState || {};
        this.eventHandlers = new Map();
    }

    // Initialize the service
    async initialize() {
        if (this.initialized) return;

        try {
            // Initialize database service
            await dbService.initialize();

            // Register event handlers for UI updates
            this.registerEventHandlers();

            // Set initialized flag
            this.initialized = true;

            console.log('Database Frontend Service initialized');
            return true;
        } catch (error) {
            console.error('Failed to initialize Database Frontend Service:', error);
            throw error;
        }
    }

    // Register event handlers for WebSocket events
    registerEventHandlers() {
        // User events
        this.registerEventHandler('USER_UPDATE', this.handleUserUpdate.bind(this));
        this.registerEventHandler('USER_UPDATE_STATUS', this.handleUserStatusUpdate.bind(this));

        // Guild events
        this.registerEventHandler('GUILD_JOIN', this.handleGuildJoin.bind(this));
        this.registerEventHandler('GUILD_LEAVE', this.handleGuildLeave.bind(this));
        this.registerEventHandler('GUILD_UPDATE', this.handleGuildUpdate.bind(this));
        this.registerEventHandler('GUILD_MEMBER_JOIN', this.handleGuildMemberJoin.bind(this));
        this.registerEventHandler('GUILD_MEMBER_LEAVE', this.handleGuildMemberLeave.bind(this));

        // Channel events
        this.registerEventHandler('CHANNEL_CREATE', this.handleChannelCreate.bind(this));
        this.registerEventHandler('CHANNEL_UPDATE', this.handleChannelUpdate.bind(this));
        this.registerEventHandler('CHANNEL_DELETE', this.handleChannelDelete.bind(this));

        // Message events
        this.registerEventHandler('MESSAGE_RECEIVED', this.handleMessageReceived.bind(this));
        this.registerEventHandler('MESSAGE_UPDATE', this.handleMessageUpdate.bind(this));
        this.registerEventHandler('MESSAGE_DELETE', this.handleMessageDelete.bind(this));

        // Typing events
        this.registerEventHandler('TYPING_START', this.handleTypingStart.bind(this));
    }

    // Register event handler
    registerEventHandler(eventType, handler) {
        this.eventHandlers.set(eventType, handler);
    }

    // Process a WebSocket event
    async processEvent(eventType, eventData) {
        try {
            const handler = this.eventHandlers.get(eventType);
            if (handler) {
                // Save to database first
                await dbService.processEvent(eventType, eventData);

                // Then update UI
                await handler(eventData);
                return true;
            } else {
                console.warn(`No handler registered for event type: ${eventType}`);
                return false;
            }
        } catch (error) {
            console.error(`Error processing event ${eventType}:`, error);
            return false;
        }
    }

    // API Fetch Methods (using database)

    // Fetch guilds
    async fetchGuilds() {
        try {
            // First try to get from database
            const guilds = await dbService.getGuilds();

            // Update current state
            this.currentState.servers = guilds;

            // If needed, fetch from API to update database
            window.api.fetchGuilds().catch(error => {
                console.warn('Failed to fetch guilds from API:', error);
            });

            return guilds;
        } catch (dbError) {
            console.warn('Failed to fetch guilds from database:', dbError);

            // Fall back to API
            return window.api.fetchGuilds();
        }
    }

    // Fetch guild channels
    async fetchGuildChannels(guildId) {
        try {
            // First try to get from database
            const channels = await dbService.getChannels(guildId);

            // Update current state
            this.currentState.channels = channels;

            // If needed, fetch from API to update database
            window.api.fetchGuildChannels(guildId).catch(error => {
                console.warn('Failed to fetch guild channels from API:', error);
            });

            return channels;
        } catch (dbError) {
            console.warn('Failed to fetch guild channels from database:', dbError);

            // Fall back to API
            return window.api.fetchGuildChannels(guildId);
        }
    }

    // Fetch guild members
    async fetchGuildMembers(guildId) {
        try {
            // First try to get from database
            const members = await dbService.getGuildMembers(guildId);

            // Update current state
            this.currentState.users = members;

            // If needed, fetch from API to update database
            window.api.fetchGuildMembers(guildId).catch(error => {
                console.warn('Failed to fetch guild members from API:', error);
            });

            return members;
        } catch (dbError) {
            console.warn('Failed to fetch guild members from database:', dbError);

            // Fall back to API
            return window.api.fetchGuildMembers(guildId);
        }
    }

    // Fetch channel messages
    async fetchMessages(channelId, options = {}) {
        try {
            // First try to get from database
            const messages = await dbService.getMessages(channelId, options);

            // Update current state
            this.currentState.messages = messages;

            // If needed, fetch from API to update database
            window.api.fetchMessages(channelId, options).catch(error => {
                console.warn('Failed to fetch messages from API:', error);
            });

            return messages;
        } catch (dbError) {
            console.warn('Failed to fetch messages from database:', dbError);

            // Fall back to API
            return window.api.fetchMessages(channelId, options);
        }
    }

    // Fetch DM messages
    async fetchDmMessages(userId, options = {}) {
        try {
            // First try to get from database
            const messages = await dbService.fetchDmMessages(userId, options);

            // Update current state
            this.currentState.messages = messages;

            // If needed, fetch from API to update database
            window.api.fetchDmMessages(userId, options).catch(error => {
                console.warn('Failed to fetch DM messages from API:', error);
            });

            return messages;
        } catch (dbError) {
            console.warn('Failed to fetch DM messages from database:', dbError);

            // Fall back to API
            return window.api.fetchDmMessages(userId, options);
        }
    }

    // Fetch DM users
    async fetchDms() {
        try {
            // First try to get from database
            const dmUsers = await dbService.getDmUsers();

            // Update current state
            this.currentState.dmUsers = dmUsers;

            // If needed, fetch from API to update database
            window.api.fetchDMs().catch(error => {
                console.warn('Failed to fetch DMs from API:', error);
            });

            return dmUsers;
        } catch (dbError) {
            console.warn('Failed to fetch DMs from database:', dbError);

            // Fall back to API
            return window.api.fetchDMs();
        }
    }

    // Fetch bot info
    async fetchBotUser() {
        try {
            // First try to get from database
            const botUser = await dbService.getUserById(this.currentState.botUserId || '@me');

            if (botUser) {
                // Update current state
                this.currentState.botUser = botUser;

                return botUser;
            }

            // If not found in database, fetch from API
            const apiBotUser = await window.api.fetchBotUser();

            // Save to database
            if (apiBotUser) {
                await dbService.saveUser(apiBotUser);
                this.currentState.botUser = apiBotUser;
                this.currentState.botUserId = apiBotUser.id;
            }

            return apiBotUser;
        } catch (error) {
            console.error('Failed to fetch bot user:', error);
            return window.api.fetchBotUser();
        }
    }

    // Send a message
    async sendMessage(channelId, content) {
        try {
            // Always use the API for sending messages
            const result = await window.api.sendMessage(channelId, content);

            // Create optimistic message update
            const optimisticMessage = {
                id: result?.id || `temp-${Date.now()}`,
                channelId: channelId,
                content: content,
                timestamp: Date.now(),
                author: this.currentState.botUser,
                attachments: [],
                embeds: []
            };

            // Add to state temporarily
            if (!result?.id) {
                this.currentState.messages.push(optimisticMessage);

                // Update UI
                if (typeof window.ui.renderMessages === 'function') {
                    window.ui.renderMessages();
                }
            }

            return result;
        } catch (error) {
            console.error('Error sending message:', error);
            throw error;
        }
    }

    // Event handlers

    // Handle user update
    async handleUserUpdate(data) {
        try {
            // Update user in the database
            await dbService.saveUser(data);

            // Update our local state
            if (this.currentState.dmUsers) {
                const dmUserIndex = this.currentState.dmUsers.findIndex(user => user.id === data.id);
                if (dmUserIndex !== -1) {
                    this.currentState.dmUsers[dmUserIndex] = { ...this.currentState.dmUsers[dmUserIndex], ...data };

                    // Update UI
                    if (window.currentState.isDmView && typeof window.ui.renderDmList === 'function') {
                        window.ui.renderDmList();
                    }
                }
            }

            if (this.currentState.users) {
                const userIndex = this.currentState.users.findIndex(user => user.id === data.id);
                if (userIndex !== -1) {
                    this.currentState.users[userIndex] = { ...this.currentState.users[userIndex], ...data };

                    // Update UI
                    if (!window.currentState.isDmView && typeof window.ui.renderUsers === 'function') {
                        window.ui.renderUsers();
                    }
                }
            }

            return true;
        } catch (error) {
            console.error('Error handling user update:', error);
            return false;
        }
    }

    // Handle user status update
    async handleUserStatusUpdate(data) {
        try {
            const userId = data.userId;
            const newStatus = data.newStatus.toLowerCase();

            // Update database
            await dbService.updateUserStatus(userId, newStatus);

            // Update local state
            if (this.currentState.dmUsers) {
                const dmUserIndex = this.currentState.dmUsers.findIndex(user => user.id === userId);
                if (dmUserIndex !== -1) {
                    this.currentState.dmUsers[dmUserIndex].status = newStatus;

                    // Update UI
                    if (window.currentState.isDmView && typeof window.ui.renderDmList === 'function') {
                        window.ui.renderDmList();
                    }
                }
            }

            if (this.currentState.users) {
                const userIndex = this.currentState.users.findIndex(user => user.id === userId);
                if (userIndex !== -1) {
                    this.currentState.users[userIndex].status = newStatus;

                    // Update UI
                    if (!window.currentState.isDmView && typeof window.ui.renderUsers === 'function') {
                        window.ui.renderUsers();
                    }
                }
            }

            return true;
        } catch (error) {
            console.error('Error handling user status update:', error);
            return false;
        }
    }

    // Handle guild join
    async handleGuildJoin(data) {
        try {
            // Update database
            await dbService.saveGuild(data);

            // Update local state
            const guild = {
                id: data.id,
                name: data.name,
                iconUrl: data.iconUrl,
                memberCount: data.memberCount
            };

            if (!this.currentState.servers) {
                this.currentState.servers = [];
            }

            // Check if guild already exists
            const guildIndex = this.currentState.servers.findIndex(server => server.id === guild.id);
            if (guildIndex !== -1) {
                this.currentState.servers[guildIndex] = { ...this.currentState.servers[guildIndex], ...guild };
            } else {
                this.currentState.servers.push(guild);
            }

            // Update UI
            if (typeof window.ui.renderServers === 'function') {
                window.ui.renderServers();
            }

            return true;
        } catch (error) {
            console.error('Error handling guild join:', error);
            return false;
        }
    }

    // Handle guild leave
    async handleGuildLeave(data) {
        try {
            // Remove guild from local state
            if (this.currentState.servers) {
                this.currentState.servers = this.currentState.servers.filter(server => server.id !== data.id);

                // Update UI
                if (typeof window.ui.renderServers === 'function') {
                    window.ui.renderServers();
                }
            }

            return true;
        } catch (error) {
            console.error('Error handling guild leave:', error);
            return false;
        }
    }

    // Handle guild update
    async handleGuildUpdate(data) {
        // Same as guild join
        return this.handleGuildJoin(data);
    }

    // Handle guild member join
    async handleGuildMemberJoin(data) {
        try {
            // Only update if we're viewing the relevant guild
            if (this.currentState.selectedServerId === data.guildId) {
                // Get member data
                const memberData = data.member || {
                    id: data.userId,
                    username: data.userName,
                    displayName: data.displayName || data.userName
                };

                // Add to local state
                if (!this.currentState.users) {
                    this.currentState.users = [];
                }

                const memberIndex = this.currentState.users.findIndex(user => user.id === memberData.id);
                if (memberIndex !== -1) {
                    this.currentState.users[memberIndex] = { ...this.currentState.users[memberIndex], ...memberData };
                } else {
                    this.currentState.users.push(memberData);
                }

                // Update UI
                if (!window.currentState.isDmView && typeof window.ui.renderUsers === 'function') {
                    window.ui.renderUsers();
                }
            }

            return true;
        } catch (error) {
            console.error('Error handling guild member join:', error);
            return false;
        }
    }

    // Handle guild member leave
    async handleGuildMemberLeave(data) {
        try {
            // Only update if we're viewing the relevant guild
            if (this.currentState.selectedServerId === data.guildId) {
                // Remove from local state
                if (this.currentState.users) {
                    this.currentState.users = this.currentState.users.filter(user => user.id !== data.userId);

                    // Update UI
                    if (!window.currentState.isDmView && typeof window.ui.renderUsers === 'function') {
                        window.ui.renderUsers();
                    }
                }
            }

            return true;
        } catch (error) {
            console.error('Error handling guild member leave:', error);
            return false;
        }
    }

    // Handle channel create
    async handleChannelCreate(data) {
        try {
            // Only update if we're viewing the relevant guild
            if (this.currentState.selectedServerId === data.guildId) {
                // Create channel object
                const channel = {
                    id: data.channelId,
                    name: data.channelName,
                    type: data.channelType,
                    parentId: data.parentId,
                    position: data.position,
                    topic: data.topic
                };

                // Add to local state
                if (!this.currentState.channels) {
                    this.currentState.channels = [];
                }

                const channelIndex = this.currentState.channels.findIndex(c => c.id === channel.id);
                if (channelIndex !== -1) {
                    this.currentState.channels[channelIndex] = { ...this.currentState.channels[channelIndex], ...channel };
                } else {
                    this.currentState.channels.push(channel);
                }

                // Update UI
                if (typeof window.ui.renderChannels === 'function') {
                    window.ui.renderChannels();
                }
            }

            return true;
        } catch (error) {
            console.error('Error handling channel create:', error);
            return false;
        }
    }

    // Handle channel update
    async handleChannelUpdate(data) {
        // Same as channel create
        return this.handleChannelCreate(data);
    }

    // Handle channel delete
    async handleChannelDelete(data) {
        try {
            // Only update if we're viewing the relevant guild
            if (this.currentState.selectedServerId === data.guildId) {
                // Remove from local state
                if (this.currentState.channels) {
                    this.currentState.channels = this.currentState.channels.filter(channel => channel.id !== data.channelId);

                    // If this was the selected channel, select another one
                    if (this.currentState.selectedChannelId === data.channelId) {
                        this.currentState.selectedChannelId = null;

                        // Find another channel to select
                        const textChannel = this.currentState.channels.find(channel => channel.type === 'TEXT');
                        if (textChannel) {
                            window.ui.selectChannel(textChannel.id);
                        } else {
                            // Clear message area
                            this.currentState.messages = [];
                            if (typeof window.ui.renderMessages === 'function') {
                                window.ui.renderMessages();
                            }
                        }
                    }

                    // Update UI
                    if (typeof window.ui.renderChannels === 'function') {
                        window.ui.renderChannels();
                    }
                }
            }

            return true;
        } catch (error) {
            console.error('Error handling channel delete:', error);
            return false;
        }
    }

    // Handle message received
    async handleMessageReceived(data) {
        try {
            // Check if this message belongs to the current view
            const isCurrentChannel = this.currentState.selectedChannelId === data.channelId;
            const isCurrentDm = this.currentState.isDmView &&
                (data.author?.id === this.currentState.selectedDmUserId ||
                    data.recipientId === this.currentState.selectedDmUserId);

            if (isCurrentChannel || isCurrentDm) {
                // Format the message
                const message = {
                    id: data.id,
                    channelId: data.channelId,
                    content: data.content,
                    timestamp: data.timestamp,
                    author: data.author,
                    attachments: data.attachments || [],
                    embeds: data.embeds || []
                };

                // Add to local state
                if (!this.currentState.messages) {
                    this.currentState.messages = [];
                }

                // Check if message already exists
                const messageIndex = this.currentState.messages.findIndex(msg => msg.id === message.id);
                if (messageIndex !== -1) {
                    this.currentState.messages[messageIndex] = { ...this.currentState.messages[messageIndex], ...message };
                } else {
                    this.currentState.messages.push(message);
                }

                // Sort messages by timestamp
                this.currentState.messages.sort((a, b) => a.timestamp - b.timestamp);

                // Update UI
                if (typeof window.ui.renderMessages === 'function') {
                    window.ui.renderMessages();
                }

                // Scroll to bottom if already at bottom
                const messagesContainer = document.getElementById('messages-container');
                if (messagesContainer) {
                    const isAtBottom = messagesContainer.scrollHeight - messagesContainer.scrollTop <= messagesContainer.clientHeight + 100;
                    if (isAtBottom) {
                        messagesContainer.scrollTop = messagesContainer.scrollHeight;
                    }
                }
            }

            // Update DM list if this is a DM
            if (!data.guildId && data.author) {
                await this.fetchDms();
            }

            return true;
        } catch (error) {
            console.error('Error handling message received:', error);
            return false;
        }
    }

    // Handle message update
    async handleMessageUpdate(data) {
        // Similar to message received, but mark as edited
        try {
            // Check if this message belongs to the current view
            const isCurrentChannel = this.currentState.selectedChannelId === data.channelId;
            const isCurrentDm = this.currentState.isDmView &&
                (data.author?.id === this.currentState.selectedDmUserId ||
                    data.recipientId === this.currentState.selectedDmUserId);

            if (isCurrentChannel || isCurrentDm) {
                // Update message in local state
                if (this.currentState.messages) {
                    const messageIndex = this.currentState.messages.findIndex(msg => msg.id === data.id);
                    if (messageIndex !== -1) {
                        // Update message
                        this.currentState.messages[messageIndex] = {
                            ...this.currentState.messages[messageIndex],
                            ...data,
                            edited: true,
                            editedTimestamp: data.editedTimestamp || Date.now()
                        };

                        // Update UI
                        if (typeof window.ui.renderMessages === 'function') {
                            window.ui.renderMessages();
                        }
                    }
                }
            }

            return true;
        } catch (error) {
            console.error('Error handling message update:', error);
            return false;
        }
    }

    // Handle message delete
    async handleMessageDelete(data) {
        try {
            // Remove message from local state
            if (this.currentState.messages) {
                this.currentState.messages = this.currentState.messages.filter(msg => msg.id !== data.messageId);

                // Update UI
                if (typeof window.ui.renderMessages === 'function') {
                    window.ui.renderMessages();
                }
            }

            return true;
        } catch (error) {
            console.error('Error handling message delete:', error);
            return false;
        }
    }

    // Handle typing start
    async handleTypingStart(data) {
        try {
            // Check if typing indicator is for the current channel/DM
            const isRelevantChannel = this.currentState.selectedChannelId === data.channelId;
            const isRelevantDM = this.currentState.isDmView && this.currentState.selectedDmUserId === data.userId;

            if (isRelevantChannel || isRelevantDM) {
                // Show typing indicator in UI
                if (typeof window.ui.showTypingIndicator === 'function') {
                    window.ui.showTypingIndicator(data.userId);
                } else {
                    // Fallback implementation
                    const messagesContainer = document.getElementById('messages-container');
                    if (messagesContainer) {
                        let typingIndicator = messagesContainer.querySelector('.typing-indicator');

                        if (!typingIndicator) {
                            typingIndicator = document.createElement('div');
                            typingIndicator.className = 'typing-indicator';
                            messagesContainer.appendChild(typingIndicator);
                        }

                        // Find username for the typing user
                        let username = 'Someone';
                        const user = this.findUserById(data.userId);
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
                }
            }

            return true;
        } catch (error) {
            console.error('Error handling typing start:', error);
            return false;
        }
    }

    // Helper function to find a user by ID
    findUserById(userId) {
        // Search in users and dmUsers
        let user = null;

        if (this.currentState.users) {
            user = this.currentState.users.find(u => u.id === userId);
        }

        if (!user && this.currentState.dmUsers) {
            user = this.currentState.dmUsers.find(u => u.id === userId);
        }

        return user;
    }

    // Replace the existing API methods with database-backed versions
    patchWindowApi() {
        // Save original methods
        const originalApi = { ...window.api };

        // Patch API methods
        window.api.fetchGuilds = this.fetchGuilds.bind(this);
        window.api.fetchGuildChannels = this.fetchGuildChannels.bind(this);
        window.api.fetchGuildMembers = this.fetchGuildMembers.bind(this);
        window.api.fetchMessages = this.fetchMessages.bind(this);
        window.api.fetchDmMessages = this.fetchDmMessages.bind(this);
        window.api.fetchDMs = this.fetchDms.bind(this);
        window.api.fetchBotUser = this.fetchBotUser.bind(this);
        window.api.sendMessage = this.sendMessage.bind(this);

        // Store original API for fallback
        this.originalApi = originalApi;

        console.log('API methods patched to use database');
    }

    // Handle WebSocket message
    handleWebSocketMessage(message) {
        try {
            console.log("WebSocket message received:", message.data);
            const data = JSON.parse(message.data);
            const eventType = data.type;
            const eventData = data.data;

            console.log(`Processing event ${eventType} with data:`, eventData);

            // Process event
            this.processEvent(eventType, eventData);
        } catch (error) {
            console.error('Error handling WebSocket message:', error);
        }
    }

    // Patch WebSocket to intercept events
    patchWebSocket() {
        const originalWebSocket = window.WebSocket;
        const self = this;

        // Replace WebSocket constructor
        window.WebSocket = function(url, protocols) {
            const socket = new originalWebSocket(url, protocols);

            // Intercept messages
            const originalOnMessage = socket.onmessage;
            socket.onmessage = function(event) {
                // Process event in our service
                self.handleWebSocketMessage(event);

                // Call original handler
                if (originalOnMessage) {
                    originalOnMessage.call(this, event);
                }
            };

            return socket;
        };

        // Copy static properties
        Object.setPrototypeOf(window.WebSocket, originalWebSocket);

        console.log('WebSocket patched to intercept events');
    }
}

// Create a singleton instance
const dbFrontendService = new DatabaseFrontendService();

// Initialize the service when the page loads
window.addEventListener('DOMContentLoaded', async () => {
    try {
        await dbFrontendService.initialize();

        // Patch API methods
        dbFrontendService.patchWindowApi();

        // Patch WebSocket
        dbFrontendService.patchWebSocket();

        console.log('Database Frontend Service ready');
    } catch (error) {
        console.error('Failed to initialize Database Frontend Service:', error);
    }
});

export default dbFrontendService;
/**
 * Discord API Service
 * This service handles the interaction with the Discord API
 */

// Immediately invoked function expression to avoid global namespace pollution
(function() {
    // Create the DiscordApiService class
    class DiscordApiService {
        constructor() {
            this.eventHandlers = {};
            this.botUserId = null;
            this.isConnected = false;

            // Initialize database integration
            this.dbService = window.dbService;

            console.log('DiscordApiService instance created');
        }

        /**
         * Initialize the API service
         */
        async initialize() {
            try {
                console.log('Initializing DiscordApiService');

                // Initialize database service if available
                if (this.dbService && typeof this.dbService.initialize === 'function') {
                    try {
                        await this.dbService.initialize();
                        console.log('Database service initialized');
                    } catch (dbError) {
                        console.warn('Database service initialization failed, continuing without database:', dbError);
                    }
                }

                // Register event handlers
                this.registerEventHandlers();

                console.log('DiscordApiService initialized successfully');
                return true;
            } catch (error) {
                console.error('Error initializing DiscordApiService:', error);
                return false;
            }
        }

        /**
         * Register event handlers for Discord events
         */
        registerEventHandlers() {
            // User events
            this.registerEventHandler('USER_UPDATE', this.handleUserUpdate.bind(this));
            this.registerEventHandler('USER_UPDATE_STATUS', this.handleUserStatusUpdate.bind(this));

            // Message events
            this.registerEventHandler('MESSAGE_RECEIVED', this.handleMessageReceived.bind(this));
            this.registerEventHandler('MESSAGE_UPDATE', this.handleMessageUpdate.bind(this));
            this.registerEventHandler('MESSAGE_DELETE', this.handleMessageDelete.bind(this));

            // Typing events
            this.registerEventHandler('TYPING_START', this.handleTypingStart.bind(this));

            console.log('Event handlers registered');
        }

        /**
         * Register an event handler
         */
        registerEventHandler(eventType, handler) {
            this.eventHandlers[eventType] = handler;
        }

        /**
         * Process a Discord event
         */
        async processEvent(eventType, eventData) {
            try {
                console.log(`Processing event: ${eventType}`, eventData);

                const handler = this.eventHandlers[eventType];
                if (handler) {
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

        /**
         * Set the bot user ID
         */
        setBotUserId(id) {
            this.botUserId = id;
            console.log(`Set bot user ID: ${id}`);

            // Update in database service if available
            if (this.dbService && typeof this.dbService.setBotUserId === 'function') {
                this.dbService.setBotUserId(id);
            }
        }

        // API Methods

        /**
         * Fetch the bot user information
         */
        async fetchBotUser() {
            try {
                console.log('Fetching bot user info');

                const response = await fetch(`${window.API_BASE_URL}/bot/info`);
                if (!response.ok) {
                    throw new Error(`Failed to fetch bot info: ${response.status} ${response.statusText}`);
                }

                const botUser = await response.json();
                console.log('Bot user info:', botUser);

                // Update local state
                window.currentState.botUser = botUser;
                this.setBotUserId(botUser.id);

                return botUser;
            } catch (error) {
                console.error('Error fetching bot user:', error);
                throw error;
            }
        }

        /**
         * Fetch all guilds
         */
        async fetchGuilds() {
            try {
                console.log('Fetching guilds');

                const response = await fetch(`${window.API_BASE_URL}/guilds`);
                if (!response.ok) {
                    throw new Error(`Failed to fetch guilds: ${response.status} ${response.statusText}`);
                }

                const guilds = await response.json();
                console.log(`Fetched ${guilds.length} guilds`);

                // Update local state
                window.currentState.servers = guilds;

                // Update UI if needed
                if (typeof window.ui.renderServers === 'function') {
                    window.ui.renderServers();
                }

                return guilds;
            } catch (error) {
                console.error('Error fetching guilds:', error);
                throw error;
            }
        }

        /**
         * Fetch channels for a guild
         */
        async fetchGuildChannels(guildId) {
            try {
                console.log(`Fetching channels for guild: ${guildId}`);

                const response = await fetch(`${window.API_BASE_URL}/guilds/${guildId}/channels`);
                if (!response.ok) {
                    throw new Error(`Failed to fetch channels: ${response.status} ${response.statusText}`);
                }

                const channels = await response.json();
                console.log(`Fetched ${channels.length} channels for guild ${guildId}`);

                // Update local state
                window.currentState.channels = channels;

                // Update UI if needed
                if (typeof window.ui.renderChannels === 'function') {
                    window.ui.renderChannels();
                }

                return channels;
            } catch (error) {
                console.error(`Error fetching channels for guild ${guildId}:`, error);
                throw error;
            }
        }

        /**
         * Fetch messages for a channel
         */
        async fetchMessages(channelId, options = {}) {
            try {
                console.log(`Fetching messages for channel: ${channelId}`, options);

                // Build query string from options
                const params = new URLSearchParams();
                if (options.limit) params.append('limit', options.limit);
                if (options.before) params.append('before', options.before);
                if (options.after) params.append('after', options.after);

                const response = await fetch(`${window.API_BASE_URL}/channels/${channelId}/messages?${params.toString()}`);
                if (!response.ok) {
                    throw new Error(`Failed to fetch messages: ${response.status} ${response.statusText}`);
                }

                const messages = await response.json();
                console.log(`Fetched ${messages.length} messages for channel ${channelId}`);

                // Update local state
                window.currentState.messages = messages;

                // Update UI if needed
                if (typeof window.ui.renderMessages === 'function') {
                    window.ui.renderMessages();
                }

                return messages;
            } catch (error) {
                console.error(`Error fetching messages for channel ${channelId}:`, error);
                throw error;
            }
        }

        /**
         * Fetch DM messages for a user
         */
        async fetchDmMessages(userId, options = {}) {
            try {
                console.log(`Fetching DM messages for user: ${userId}`, options);

                // First get the DM channel
                const dmResponse = await fetch(`${window.API_BASE_URL}/users/${userId}/dm`);
                if (!dmResponse.ok) {
                    throw new Error(`Failed to fetch DM channel: ${dmResponse.status} ${dmResponse.statusText}`);
                }

                const dmChannel = await dmResponse.json();
                console.log(`Got DM channel for user ${userId}: ${dmChannel.id}`);

                // Then fetch messages for this channel
                window.currentState.selectedChannelId = dmChannel.id;
                return await this.fetchMessages(dmChannel.id, options);
            } catch (error) {
                console.error(`Error fetching DM messages for user ${userId}:`, error);
                throw error;
            }
        }

        /**
         * Fetch DM users
         */
        async fetchDMs() {
            try {
                console.log('Fetching DM users');

                const response = await fetch(`${window.API_BASE_URL}/users?all=true`);
                if (!response.ok) {
                    throw new Error(`Failed to fetch DM users: ${response.status} ${response.statusText}`);
                }

                const users = await response.json();
                console.log(`Fetched ${users.length} DM users`);

                // Update local state
                window.currentState.dmUsers = users;

                // Update UI if needed
                if (window.currentState.isDmView && typeof window.ui.renderDmList === 'function') {
                    window.ui.renderDmList();
                }

                return users;
            } catch (error) {
                console.error('Error fetching DM users:', error);
                throw error;
            }
        }

        /**
         * Fetch guild members
         */
        async fetchGuildMembers(guildId) {
            try {
                console.log(`Fetching members for guild: ${guildId}`);

                const response = await fetch(`${window.API_BASE_URL}/guilds/${guildId}/members`);
                if (!response.ok) {
                    throw new Error(`Failed to fetch guild members: ${response.status} ${response.statusText}`);
                }

                const members = await response.json();
                console.log(`Fetched ${members.length} members for guild ${guildId}`);

                // Update local state
                window.currentState.users = members;

                // Update UI if needed
                if (!window.currentState.isDmView && typeof window.ui.renderUsers === 'function') {
                    window.ui.renderUsers();
                }

                return members;
            } catch (error) {
                console.error(`Error fetching members for guild ${guildId}:`, error);
                throw error;
            }
        }

        /**
         * Send a message to a channel
         */
        async sendMessage(channelId, content) {
            try {
                console.log(`Sending message to channel: ${channelId}`, content);

                // Create a message DTO
                const messageData = {
                    content: content
                };

                // Send to Discord API
                const response = await fetch(`${window.API_BASE_URL}/channels/${channelId}/messages`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(messageData)
                });

                if (!response.ok) {
                    throw new Error(`Failed to send message: ${response.status} ${response.statusText}`);
                }

                const message = await response.json();
                console.log('Message sent successfully:', message);

                // Attempt to save to database if available
                if (this.dbService && typeof this.dbService.saveMessage === 'function') {
                    try {
                        // Ensure message has full data
                        message.channelId = channelId;
                        if (!message.author && window.currentState.botUser) {
                            message.author = window.currentState.botUser;
                        }

                        await this.dbService.saveMessage(message);
                    } catch (dbError) {
                        console.warn('Failed to save message to database:', dbError);
                    }
                }

                return message;
            } catch (error) {
                console.error(`Error sending message to channel ${channelId}:`, error);
                throw error;
            }
        }

        /**
         * Create a DM channel with a user
         */
        async createDMChannel(userId) {
            try {
                console.log(`Creating DM channel with user: ${userId}`);

                const response = await fetch(`${window.API_BASE_URL}/users/${userId}/dm`, {
                    method: 'POST',
                });

                if (!response.ok) {
                    throw new Error(`Failed to create DM channel: ${response.status} ${response.statusText}`);
                }

                const dmChannel = await response.json();
                console.log(`Created DM channel: ${dmChannel.id}`);

                // Save to database if available
                if (this.dbService && typeof this.dbService.saveDmChannel === 'function') {
                    try {
                        await this.dbService.saveDmChannel(dmChannel.id, userId);
                    } catch (dbError) {
                        console.warn('Failed to save DM channel to database:', dbError);
                    }
                }

                return dmChannel;
            } catch (error) {
                console.error(`Error creating DM channel with user ${userId}:`, error);
                throw error;
            }
        }

        // Event Handlers

        /**
         * Handle user update event
         */
        async handleUserUpdate(data) {
            try {
                console.log('Handling user update:', data);

                // Try to save to database
                if (this.dbService && typeof this.dbService.saveUser === 'function') {
                    try {
                        await this.dbService.saveUser(data);
                    } catch (dbError) {
                        console.warn('Failed to save user update to database:', dbError);
                    }
                }

                // Update local state
                if (window.currentState.dmUsers) {
                    const userIndex = window.currentState.dmUsers.findIndex(user => user.id === data.id);
                    if (userIndex !== -1) {
                        window.currentState.dmUsers[userIndex] = {
                            ...window.currentState.dmUsers[userIndex],
                            ...data
                        };
                    }
                }

                if (window.currentState.users) {
                    const userIndex = window.currentState.users.findIndex(user => user.id === data.id);
                    if (userIndex !== -1) {
                        window.currentState.users[userIndex] = {
                            ...window.currentState.users[userIndex],
                            ...data
                        };
                    }
                }

                // Update UI
                if (window.currentState.isDmView && typeof window.ui.renderDmList === 'function') {
                    window.ui.renderDmList();
                } else if (!window.currentState.isDmView && typeof window.ui.renderUsers === 'function') {
                    window.ui.renderUsers();
                }

                return true;
            } catch (error) {
                console.error('Error handling user update:', error);
                return false;
            }
        }

        /**
         * Handle user status update event
         */
        async handleUserStatusUpdate(data) {
            try {
                console.log('Handling user status update:', data);

                const userId = data.userId;
                const newStatus = data.newStatus?.toLowerCase() || 'offline';

                // Try to save to database
                if (this.dbService) {
                    try {
                        // Update status in database
                        await fetch(`${window.API_BASE_URL}/db/users/${userId}/status`, {
                            method: 'PATCH',
                            headers: {
                                'Content-Type': 'application/json',
                            },
                            body: JSON.stringify({ status: newStatus })
                        });
                    } catch (dbError) {
                        console.warn('Failed to save user status to database:', dbError);
                    }
                }

                // Update local state
                if (window.currentState.dmUsers) {
                    const userIndex = window.currentState.dmUsers.findIndex(user => user.id === userId);
                    if (userIndex !== -1) {
                        window.currentState.dmUsers[userIndex].status = newStatus;
                    }
                }

                if (window.currentState.users) {
                    const userIndex = window.currentState.users.findIndex(user => user.id === userId);
                    if (userIndex !== -1) {
                        window.currentState.users[userIndex].status = newStatus;
                    }
                }

                // Update UI
                if (window.currentState.isDmView && typeof window.ui.renderDmList === 'function') {
                    window.ui.renderDmList();
                } else if (!window.currentState.isDmView && typeof window.ui.renderUsers === 'function') {
                    window.ui.renderUsers();
                }

                return true;
            } catch (error) {
                console.error('Error handling user status update:', error);
                return false;
            }
        }

        /**
         * Handle message received event
         */
        async handleMessageReceived(data) {
            try {
                console.log('Handling message received:', data);

                // Try to save to database
                if (this.dbService && typeof this.dbService.saveMessage === 'function') {
                    try {
                        await this.dbService.saveMessage(data);
                    } catch (dbError) {
                        console.warn('Failed to save message to database:', dbError);
                    }
                }

                // Check if this message belongs to the current view
                const isCurrentChannel = window.currentState.selectedChannelId === data.channelId;
                const isCurrentDm = window.currentState.isDmView &&
                    (data.author?.id === window.currentState.selectedDmUserId ||
                        data.recipientId === window.currentState.selectedDmUserId);

                if (isCurrentChannel || isCurrentDm) {
                    // Add message to current view
                    window.currentState.messages.push(data);

                    // Sort messages by timestamp
                    window.currentState.messages.sort((a, b) => a.timestamp - b.timestamp);

                    // Update UI
                    if (typeof window.ui.renderMessages === 'function') {
                        window.ui.renderMessages();
                    }

                    // Scroll to bottom if at bottom
                    const messagesContainer = document.getElementById('messages-container');
                    if (messagesContainer) {
                        const isAtBottom = messagesContainer.scrollHeight - messagesContainer.scrollTop <= messagesContainer.clientHeight + 100;
                        if (isAtBottom) {
                            messagesContainer.scrollTop = messagesContainer.scrollHeight;
                        }
                    }
                }

                return true;
            } catch (error) {
                console.error('Error handling message received:', error);
                return false;
            }
        }

        /**
         * Handle message update event
         */
        async handleMessageUpdate(data) {
            try {
                console.log('Handling message update:', data);

                // Try to save to database
                if (this.dbService && typeof this.dbService.saveMessage === 'function') {
                    try {
                        await this.dbService.saveMessage({
                            ...data,
                            edited: true,
                            editedTimestamp: data.editedTimestamp || Date.now()
                        });
                    } catch (dbError) {
                        console.warn('Failed to save message update to database:', dbError);
                    }
                }

                // Update in current view if present
                const messageIndex = window.currentState.messages.findIndex(msg => msg.id === data.id);
                if (messageIndex !== -1) {
                    window.currentState.messages[messageIndex] = {
                        ...window.currentState.messages[messageIndex],
                        ...data,
                        edited: true,
                        editedTimestamp: data.editedTimestamp || Date.now()
                    };

                    // Update UI
                    if (typeof window.ui.renderMessages === 'function') {
                        window.ui.renderMessages();
                    }
                }

                return true;
            } catch (error) {
                console.error('Error handling message update:', error);
                return false;
            }
        }

        /**
         * Handle message delete event
         */
        async handleMessageDelete(data) {
            try {
                console.log('Handling message delete:', data);

                // Try to delete from database
                if (this.dbService) {
                    try {
                        await fetch(`${window.API_BASE_URL}/db/messages/${data.messageId}`, {
                            method: 'DELETE'
                        });
                    } catch (dbError) {
                        console.warn('Failed to delete message from database:', dbError);
                    }
                }

                // Remove from current view if present
                window.currentState.messages = window.currentState.messages.filter(msg => msg.id !== data.messageId);

                // Update UI
                if (typeof window.ui.renderMessages === 'function') {
                    window.ui.renderMessages();
                }

                return true;
            } catch (error) {
                console.error('Error handling message delete:', error);
                return false;
            }
        }

        /**
         * Handle typing start event
         */
        async handleTypingStart(data) {
            try {
                console.log('Handling typing start:', data);

                // Check if typing is relevant to current view
                const isRelevantChannel = window.currentState.selectedChannelId === data.channelId;
                const isRelevantDM = window.currentState.isDmView && window.currentState.selectedDmUserId === data.userId;

                if (isRelevantChannel || isRelevantDM) {
                    // Call UI function to show typing
                    if (typeof window.ui.showTypingIndicator === 'function') {
                        window.ui.showTypingIndicator(data.userId);
                    } else {
                        // Fallback for typing indicator
                        const messagesContainer = document.getElementById('messages-container');
                        if (messagesContainer) {
                            let typingIndicator = messagesContainer.querySelector('.typing-indicator');

                            if (!typingIndicator) {
                                typingIndicator = document.createElement('div');
                                typingIndicator.className = 'typing-indicator';
                                messagesContainer.appendChild(typingIndicator);
                            }

                            // Find username
                            let username = 'Someone';
                            const userList = window.currentState.isDmView ?
                                window.currentState.dmUsers : window.currentState.users;

                            const user = userList?.find(u => u.id === data.userId);
                            if (user) {
                                username = user.displayName || user.username || 'Someone';
                            }

                            typingIndicator.innerHTML = `
                                <div class="typing-animation">
                                    <span class="dot"></span>
                                    <span class="dot"></span>
                                    <span class="dot"></span>
                                </div>
                                <span class="typing-text">${username} is typing...</span>
                            `;

                            // Auto-remove after 10 seconds
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
    }

    // Create a global instance of the service
    window.discordApi = new DiscordApiService();

    // For backwards compatibility with existing code
    window.api = window.api || {};

    // Define API methods on the global window.api object
    window.api.fetchBotUser = window.discordApi.fetchBotUser.bind(window.discordApi);
    window.api.fetchGuilds = window.discordApi.fetchGuilds.bind(window.discordApi);
    window.api.fetchGuildChannels = window.discordApi.fetchGuildChannels.bind(window.discordApi);
    window.api.fetchMessages = window.discordApi.fetchMessages.bind(window.discordApi);
    window.api.fetchDmMessages = window.discordApi.fetchDmMessages.bind(window.discordApi);
    window.api.fetchDMs = window.discordApi.fetchDMs.bind(window.discordApi);
    window.api.fetchGuildMembers = window.discordApi.fetchGuildMembers.bind(window.discordApi);
    window.api.sendMessage = window.discordApi.sendMessage.bind(window.discordApi);
    window.api.createDMChannel = window.discordApi.createDMChannel.bind(window.discordApi);

    // For module support
    if (typeof module !== 'undefined' && module.exports) {
        module.exports = window.discordApi;
    }
})();
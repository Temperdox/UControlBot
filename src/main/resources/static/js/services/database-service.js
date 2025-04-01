/**
 * Database Service for Discord Bot UI
 * This service handles communication with the backend database
 */

// Immediately invoked function expression to avoid global namespace pollution
(function() {
    // Create the DatabaseService class
    class DatabaseService {
        constructor() {
            this.initialized = false;
            this.botUserId = null;
            console.log('DatabaseService instance created');
        }

        /**
         * Initialize the database service
         */
        async initialize() {
            if (this.initialized) return;

            try {
                console.log('Initializing DatabaseService');

                // Test connection to database by fetching a small amount of data
                const response = await fetch('/api/db/users?limit=1');
                if (response.ok) {
                    this.initialized = true;
                    console.log('DatabaseService initialized successfully');
                    return true;
                } else {
                    console.error('Error connecting to database API:', await response.text());
                    return false;
                }
            } catch (error) {
                console.error('Error initializing DatabaseService:', error);
                return false;
            }
        }

        /**
         * Set the bot user ID for identifying the bot's messages
         */
        setBotUserId(id) {
            this.botUserId = id;
            console.log(`Set bot user ID: ${id}`);
        }

        /**
         * Save a message to the database
         */
        async saveMessage(message) {
            try {
                console.log('💾 Saving message to database:', message);

                if (!message || !message.id || !message.channelId) {
                    console.error('Invalid message data:', message);
                    return false;
                }

                // First ensure the channel exists
                await this.ensureChannelExists(message.channelId);

                // Then ensure the author exists
                if (message.author && message.author.id) {
                    await this.ensureUserExists(message.author.id);
                }

                // Send to database API
                const response = await fetch(`/api/db/messages`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(message)
                });

                if (!response.ok) {
                    console.error('Error saving message to database:', await response.text());
                    return false;
                }

                return true;
            } catch (error) {
                console.error('Error saving message:', error);
                return false;
            }
        }

        /**
         * Save a DM channel to the database
         */
        async saveDmChannel(channelId, userId, lastMessageId = null) {
            try {
                console.log(`Saving DM channel: ${channelId} for user: ${userId}`);

                // Prepare data
                const dmData = {
                    id: channelId,
                    userId: userId,
                    lastMessageId: lastMessageId
                };

                // Send to database API
                const response = await fetch(`/api/db/dm-channels`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(dmData)
                });

                if (!response.ok) {
                    console.error('Error saving DM channel to database:', await response.text());
                    return false;
                }

                return true;
            } catch (error) {
                console.error('Error saving DM channel:', error);
                return false;
            }
        }

        /**
         * Ensure a channel exists in the database
         */
        async ensureChannelExists(channelId, name = null) {
            try {
                // Try to fetch channel first
                const response = await fetch(`/api/db/channels/${channelId}`);
                if (response.ok) {
                    // Channel exists
                    return true;
                }

                console.log(`Creating placeholder channel: ${channelId}`);

                // Create placeholder channel
                const channelData = {
                    id: channelId,
                    name: name || "Unknown Channel",
                    type: "UNKNOWN"
                };

                const createResponse = await fetch(`/api/db/channels`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(channelData)
                });

                if (!createResponse.ok) {
                    console.error('Error creating placeholder channel:', await createResponse.text());
                    return false;
                }

                return true;
            } catch (error) {
                console.error('Error ensuring channel exists:', error);
                return false;
            }
        }

        /**
         * Ensure a user exists in the database
         */
        async ensureUserExists(userId, username = null) {
            try {
                // Try to fetch user first
                const response = await fetch(`/api/db/users/${userId}`);
                if (response.ok) {
                    // User exists
                    return true;
                }

                console.log(`Creating placeholder user: ${userId}`);

                // Create placeholder user
                const userData = {
                    id: userId,
                    username: username || "Unknown User",
                    status: "offline"
                };

                const createResponse = await fetch(`/api/db/users`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(userData)
                });

                if (!createResponse.ok) {
                    console.error('Error creating placeholder user:', await createResponse.text());
                    return false;
                }

                return true;
            } catch (error) {
                console.error('Error ensuring user exists:', error);
                return false;
            }
        }

        /**
         * Get all users from the database
         */
        async getUsers(options = {}) {
            try {
                // Build query string from options
                const params = new URLSearchParams();
                if (options.limit) params.append('limit', options.limit);
                if (options.status) params.append('status', options.status);
                if (options.nameFilter) params.append('name', options.nameFilter);

                // Fetch from database API
                const response = await fetch(`/api/db/users?${params.toString()}`);
                if (!response.ok) {
                    throw new Error(`Failed to fetch users: ${response.status} ${response.statusText}`);
                }

                const users = await response.json();
                return users;
            } catch (error) {
                console.error('Error getting users:', error);
                throw error;
            }
        }

        /**
         * Get messages from a channel
         */
        async getMessages(channelId, options = {}) {
            try {
                // Build query string from options
                const params = new URLSearchParams();
                if (options.limit) params.append('limit', options.limit);
                if (options.before) params.append('before', options.before);
                if (options.after) params.append('after', options.after);

                // Fetch from database API
                const response = await fetch(`/api/db/messages/${channelId}?${params.toString()}`);
                if (!response.ok) {
                    throw new Error(`Failed to fetch messages: ${response.status} ${response.statusText}`);
                }

                const messages = await response.json();
                return messages;
            } catch (error) {
                console.error(`Error getting messages for channel ${channelId}:`, error);
                throw error;
            }
        }
    }

    // Create a singleton instance
    window.dbService = new DatabaseService();

    // For backwards compatibility and module support
    if (typeof module !== 'undefined' && module.exports) {
        module.exports = window.dbService;
    }
})();
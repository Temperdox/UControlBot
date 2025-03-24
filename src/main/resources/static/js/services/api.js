// services/api.js
window.API_BASE_URL = window.API_BASE_URL || '/api';

// State - moved from main.js
window.currentState = {
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

window.responseCache = {
    dmUsers: null,
    channels: {},
    messages: {},
    users: {},
    lastUpdated: {}
};

// Helper function for API requests
const apiRequest = async (endpoint, options = {}, useCache = true) => {
    const url = `${window.API_BASE_URL}/${endpoint}`;
    const cacheKey = `${url}-${JSON.stringify(options)}`;

    // Check if we have a cached response and it's not too old (5 minutes)
    const now = Date.now();
    const maxAge = 5 * 60 * 1000; // 5 minutes

    if (useCache && window.responseCache[cacheKey] &&
        (now - window.responseCache.lastUpdated[cacheKey] < maxAge)) {
        console.log(`Using cached response for: ${url}`);
        return window.responseCache[cacheKey];
    }

    const defaultOptions = {
        headers: {
            'Content-Type': 'application/json',
        },
        credentials: 'include',
    };

    const config = {
        ...defaultOptions,
        ...options,
        headers: {
            ...defaultOptions.headers,
            ...options.headers,
        },
    };

    try {
        console.log(`Making API request to: ${url}`);
        const response = await fetch(url, config);

        if (!response.ok) {
            const errorData = await response.json().catch(() => null);
            throw new Error(errorData?.message || `API request failed with status ${response.status}`);
        }

        const data = await response.json();

        // Cache the response
        window.responseCache[cacheKey] = data;
        window.responseCache.lastUpdated[cacheKey] = now;

        return data;
    } catch (error) {
        console.error(`API request failed: ${error.message}`);
        throw error;
    }
};

// API Functions
window.api = {
    fetchUser: async (userId) => {
        return apiRequest(`users/${userId}`);
    },

    fetchGuilds: async () => {
        try {
            const data = await apiRequest('guilds');
            window.currentState.servers = data;
            window.ui.renderServers();
            return data;
        } catch (error) {
            console.error('Error fetching guilds:', error);
            return [];
        }
    },

    fetchGuild: async (guildId) => {
        return apiRequest(`guilds/${guildId}`);
    },

    fetchGuildChannels: async (guildId) => {
        try {
            const data = await apiRequest(`guilds/${guildId}/channels`);
            window.currentState.channels = data;
            window.ui.renderChannels();
            return data;
        } catch (error) {
            console.error('Error fetching channels:', error);
            return [];
        }
    },

    fetchGuildMembers: async (guildId) => {
        try {
            const data = await apiRequest(`guilds/${guildId}/members`);
            window.currentState.users = data;
            window.ui.renderUsers();
            return data;
        } catch (error) {
            console.error('Error fetching members:', error);
            return [];
        }
    },

    fetchDMs: async () => {
        try {
            console.log("Fetching DM users...");
            const dmUsers = await apiRequest('users?dm=true');
            console.log("DM users received:", dmUsers);

            // Set initial state
            window.currentState.dmUsers = Array.isArray(dmUsers) ? dmUsers : [];

            // Fetch bot owner separately
            try {
                const owner = await apiRequest('bot/info/owner');
                console.log("Owner info received:", owner);

                if (owner && owner.id) {
                    // Set status for the owner
                    owner.status = "online";

                    // Check if owner already exists in the list
                    const existingOwnerIndex = window.currentState.dmUsers.findIndex(user => user.id === owner.id);

                    if (existingOwnerIndex !== -1) {
                        // Update existing user with owner flag
                        window.currentState.dmUsers[existingOwnerIndex].isOwner = true;
                        window.currentState.dmUsers[existingOwnerIndex].status = "online";
                    } else {
                        // Add owner to the list
                        owner.isOwner = true;
                        window.currentState.dmUsers.unshift(owner);
                    }
                }

                // Set statuses for known users based on ID
                for (const user of window.currentState.dmUsers) {
                    // Fill in missing statuses based on known user IDs
                    if (user.id === "568631703053139974") { // Your ID (Cotton Le Sergal)
                        user.status = "online";
                    } else if (user.id === "1040120006534516806") { // Deathly Eccs' ID
                        user.status = "dnd";
                    } else if (user.username === "Manifold" ||
                        (user.displayName && user.displayName === "Manifold")) {
                        user.status = "online";
                    } else {
                        // Default status if not specified
                        user.status = user.status || "offline";
                    }
                }

            } catch (ownerError) {
                console.error("Failed to fetch owner:", ownerError);
            }

            console.log("Final DM users with set statuses:", window.currentState.dmUsers);
            window.ui.renderDmList();
            return window.currentState.dmUsers;
        } catch (error) {
            console.error('Error fetching DMs:', error);
            return [];
        }
    },

    fetchMessages: async (channelId, options = {}) => {
        try {
            console.log(`Fetching messages for channel: ${channelId}`, options);

            // Use cached messages for immediate display if available
            const cacheKey = `messages-${channelId}`;
            if (window.responseCache[cacheKey] && !options.before && !options.after) {
                console.log("Using cached messages for initial display");
                window.currentState.messages = window.responseCache[cacheKey];
                window.ui.renderMessages();
            }

            // Build query parameters
            let queryParams = new URLSearchParams();
            if (options.before) queryParams.append('before', options.before);
            if (options.after) queryParams.append('after', options.after);
            if (options.limit) queryParams.append('limit', options.limit);

            const queryString = queryParams.toString();
            const endpoint = `channels/${channelId}/messages${queryString ? '?' + queryString : ''}`;

            const data = await apiRequest(endpoint, {}, false); // Don't use cache for the request itself

            // Handle pagination differently based on whether we're loading more or refreshing
            if (options.before) {
                // We're loading older messages, so prepend them to the existing list
                window.currentState.messages = [...data, ...window.currentState.messages];
            } else if (options.after) {
                // We're loading newer messages, so append them to the existing list
                window.currentState.messages = [...window.currentState.messages, ...data];
            } else {
                // Initial load, just replace all messages
                window.currentState.messages = data;
            }

            // Update cache with the latest messages
            window.responseCache[cacheKey] = window.currentState.messages;

            // Sort messages by timestamp
            window.currentState.messages.sort((a, b) => a.timestamp - b.timestamp);

            window.ui.renderMessages();
            return data;
        } catch (error) {
            console.error('Error fetching messages:', error);

            // If we have cached messages, use them when the request fails
            const cacheKey = `messages-${channelId}`;
            if (window.responseCache[cacheKey]) {
                console.log("Using cached messages due to fetch error");
                window.currentState.messages = window.responseCache[cacheKey];
                window.ui.renderMessages();
            }

            return [];
        }
    },

    fetchDmMessages: async (userId) => {
        try {
            console.log("Fetching DM messages for user ID:", userId);
            const dmChannel = await apiRequest(`users/${userId}/dm`);
            console.log("DM channel response:", dmChannel);

            if (dmChannel && dmChannel.id) {
                window.currentState.selectedChannelId = dmChannel.id;
                console.log("Setting selected channel ID:", dmChannel.id);

                // Fetch messages for this DM channel
                const messages = await apiRequest(`channels/${dmChannel.id}/messages`);
                console.log("DM messages received:", messages);

                // Sort messages by timestamp (oldest first)
                const sortedMessages = Array.isArray(messages) ? messages.sort((a, b) => a.timestamp - b.timestamp) : [];
                window.currentState.messages = sortedMessages;

                window.ui.renderMessages();
                return dmChannel;
            } else {
                console.log("No DM channel found or returned empty");
                window.currentState.messages = [];
                window.ui.renderMessages();
                return null;
            }
        } catch (error) {
            console.error('Error fetching DM channel:', error);
            window.currentState.messages = [];
            window.ui.renderMessages();
            return null;
        }
    },

    sendMessage: async (channelId, content) => {
        try {
            const result = await apiRequest(`channels/${channelId}/messages`, {
                method: 'POST',
                body: JSON.stringify({ content }),
            });

            // Refresh messages
            if (window.currentState.isDmView) {
                await window.api.fetchDmMessages(window.currentState.selectedDmUserId);
            } else {
                await window.api.fetchMessages(window.currentState.selectedChannelId);
            }

            return result;
        } catch (error) {
            console.error('Error sending message:', error);
            return null;
        }
    },

    createDMChannel: async (userId) => {
        return apiRequest(`users/${userId}/dm`, {
            method: 'POST',
        });
    },

    fetchBotUser: async () => {
        try {
            // Try with a specific endpoint first (you might need to adjust this)
            const data = await apiRequest('bot/info');
            window.currentState.botUser = data;
            return data;
        } catch (firstError) {
            console.error('Error fetching bot info:', firstError);
            try {
                // Fallback to a different endpoint if available
                console.log('Attempting fallback...');

                // Create a placeholder bot user if the API call fails
                const placeholderUser = {
                    username: 'Bot',
                    discriminator: '0000',
                    id: '0',
                    avatarUrl: null
                };
                window.currentState.botUser = placeholderUser;
                return placeholderUser;
            } catch (error) {
                console.error('Error with fallback for bot user:', error);
                return null;
            }
        }
    }
};

window.testFetchMessages = async (channelId) => {
    const url = `/api/channels/${channelId}/messages`;
    console.log(`Testing fetch messages for channel ${channelId} at URL: ${url}`);

    try {
        const response = await fetch(url, {
            headers: {
                'Content-Type': 'application/json',
            },
            credentials: 'include'
        });

        console.log(`Response status: ${response.status}`);

        if (!response.ok) {
            throw new Error(`API request failed with status ${response.status}`);
        }

        const data = await response.json();
        console.log("API response data:", data);
        return data;
    } catch (error) {
        console.error(`Error fetching messages: ${error.message}`);
        return null;
    }
};

window.inspectChannels = () => {
    console.log("Available channels:", window.currentState.channels);

    if (window.currentState.channels.length > 0) {
        const firstChannel = window.currentState.channels[0];
        console.log("First channel details:", firstChannel);
        console.log("Testing fetch with first channel ID:", firstChannel.id);
        window.testFetchMessages(firstChannel.id);
    } else {
        console.log("No channels available to test");
    }
};

// Create the window.ui namespace (moved from main.js)
window.ui = {};

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    // Initialize UI (this will be defined in ui.js)
    if (typeof window.ui.init === 'function') {
        window.ui.init();
    }

    // Initialize app
    window.app.init();
});

// Application initialization
window.app = {
    init: async function() {
        // Fetch bot info
        await window.api.fetchBotUser();

        // Fetch servers
        await window.api.fetchGuilds();

        // Start in DM view
        window.ui.switchToDmView();

        // Initialize WebSocket
        window.ui.initWebSocket();
    }
};
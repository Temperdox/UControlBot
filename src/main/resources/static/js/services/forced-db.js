// Add this to your index.html or as a separate forced-db.js file
(function() {
    console.log("Forcibly integrating database into frontend");

    // Save original API functions
    const originalFetchGuilds = window.api.fetchGuilds;
    const originalFetchChannels = window.api.fetchGuildChannels;
    const originalFetchMessages = window.api.fetchMessages;
    const originalFetchUsers = window.api.fetchDMs;
    const originalSendMessage = window.api.sendMessage;

    // Replace with database versions
    window.api.sendMessage = async function(channelId, content) {
        try {
            console.log(`Using database for sendMessage(${channelId})`);

            // Create message object
            const message = {
                id: `msg-${Date.now()}`,
                channelId: channelId,
                content: content,
                timestamp: Date.now(),
                author: window.currentState.botUser,
                attachments: [],
                embeds: []
            };

            // First try database endpoint
            const response = await fetch(`/api/db/channels/${channelId}/messages`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ content }),
            });

            if (response.ok) {
                // Also send through API to ensure delivery
                return await originalSendMessage.call(this, channelId, content);
            } else {
                throw new Error('Database endpoint failed');
            }
        } catch (error) {
            console.error(`Database message send failed, falling back to API`, error);
            return originalSendMessage.call(this, channelId, content);
        }
    };

    window.api.fetchGuilds = async function() {
        try {
            console.log("Using database for fetchGuilds");
            const response = await fetch('/api/db/guilds');
            const data = await response.json();

            // Update state
            window.currentState.servers = data;

            // Update UI if needed
            if (typeof window.ui.renderServers === 'function') {
                window.ui.renderServers();
            }

            return data;
        } catch (error) {
            console.error("Database fetch for guilds failed, falling back to API", error);
            return originalFetchGuilds.apply(this, arguments);
        }
    };

    window.api.fetchGuildChannels = async function(guildId) {
        try {
            console.log(`Using database for fetchGuildChannels(${guildId})`);
            const response = await fetch(`/api/db/channels/${guildId}`);
            const data = await response.json();

            // Update state
            window.currentState.channels = data;

            // Update UI if needed
            if (typeof window.ui.renderChannels === 'function') {
                window.ui.renderChannels();
            }

            return data;
        } catch (error) {
            console.error(`Database fetch for channels failed, falling back to API`, error);
            return originalFetchChannels.apply(this, arguments);
        }
    };

    window.api.fetchMessages = async function(channelId, options) {
        try {
            console.log(`Using database for fetchMessages(${channelId})`);
            const response = await fetch(`/api/db/messages/${channelId}`);
            const data = await response.json();

            // Update state
            window.currentState.messages = data;

            // Update UI if needed
            if (typeof window.ui.renderMessages === 'function') {
                window.ui.renderMessages();
            }

            return data;
        } catch (error) {
            console.error(`Database fetch for messages failed, falling back to API`, error);
            return originalFetchMessages.apply(this, arguments);
        }
    };

    window.api.fetchDMs = async function() {
        try {
            console.log("Using database for fetchDMs");
            const response = await fetch('/api/db/users');
            const data = await response.json();

            // Filter to just users we want for DMs
            const dmUsers = data.filter(user => !user.isBot || user.isOwner);

            // Update state
            window.currentState.dmUsers = dmUsers;

            // Update UI if needed
            if (window.currentState.isDmView && typeof window.ui.renderDmList === 'function') {
                window.ui.renderDmList();
            }

            return dmUsers;
        } catch (error) {
            console.error("Database fetch for DM users failed, falling back to API", error);
            return originalFetchUsers.apply(this, arguments);
        }
    };

    console.log("Database integration forced on window.api");
})();
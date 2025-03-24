/**
 * User DTO for Discord user entities
 * @extends DiscordDTO
 */
class UserDTO extends DiscordDTO {
    // Property keys
    static ID = 'id';
    static USERNAME = 'username';
    static DISPLAY_NAME = 'displayName';
    static AVATAR = 'avatarUrl';
    static DISCRIMINATOR = 'discriminator';
    static IS_BOT = 'isBot';
    static STATUS = 'status';

    /**
     * Create a new UserDTO
     * @param {Object} data - Initial data
     */
    constructor(data = {}) {
        super(data);
    }

    /**
     * Get user ID
     * @returns {string} - User ID
     */
    getId() {
        return this.get(UserDTO.ID);
    }

    /**
     * Get username
     * @returns {string} - Username
     */
    getUsername() {
        return this.get(UserDTO.USERNAME);
    }

    /**
     * Get display name
     * @returns {string} - Display name
     */
    getDisplayName() {
        return this.get(UserDTO.DISPLAY_NAME);
    }

    /**
     * Get avatar URL
     * @returns {string} - Avatar URL
     */
    getAvatarUrl() {
        return this.get(UserDTO.AVATAR);
    }

    /**
     * Fetch user info from API
     * @param {string} userId - User ID
     * @returns {Promise<UserDTO>} - This UserDTO with updated data
     */
    async fetchInfo(userId) {
        try {
            const response = await fetch(`/api/users/${userId}`);
            if (response.ok) {
                const data = await response.json();
                this.fromJSON(data);
            }
        } catch (error) {
            console.error('Failed to fetch user:', error);
        }
        return this;
    }
}

// Make available globally
window.UserDTO = UserDTO;
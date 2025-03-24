/**
 * Base DTO (Data Transfer Object) for Discord entities
 */
class DiscordDTO {
    /**
     * Create a new DTO
     * @param {Object} data - Initial data
     */
    constructor(data = {}) {
        this.data = data;
    }

    /**
     * Get a property value
     * @param {string} property - Property name
     * @returns {*} - Property value
     */
    get(property) {
        return this.data[property];
    }

    /**
     * Set a property value
     * @param {string} property - Property name
     * @param {*} value - Property value
     * @returns {DiscordDTO} - This DTO for chaining
     */
    set(property, value) {
        this.data[property] = value;
        return this;
    }

    /**
     * Convert to JSON
     * @returns {Object} - Data as JSON
     */
    toJSON() {
        return this.data;
    }

    /**
     * Populate from JSON
     * @param {Object|string} json - JSON data or string
     * @returns {DiscordDTO} - This DTO for chaining
     */
    fromJSON(json) {
        if (typeof json === 'string') {
            this.data = JSON.parse(json);
        } else {
            this.data = json;
        }
        return this;
    }
}

// Make available globally
window.DiscordDTO = DiscordDTO;
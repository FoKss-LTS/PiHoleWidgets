/*
 *
 *  Copyright (C) 2022 - 2025.  Reda ELFARISSI aka FoKss-LTS
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package services;

import domain.configuration.DnsBlockerConfig;
import domain.configuration.DnsBlockerType;
import services.adguard.AdGuardHomeHandler;
import services.pihole.PiHoleHandler;

/**
 * Factory for creating the appropriate DnsBlockerHandler implementation
 * based on the configured platform type.
 */
public class DnsBlockerHandlerFactory {

    /**
     * Creates a DnsBlockerHandler instance based on the platform type in the
     * configuration.
     *
     * @param config the DNS blocker configuration containing platform type and
     *               credentials
     * @return a DnsBlockerHandler implementation (PiHoleHandler or
     *         AdGuardHomeHandler)
     * @throws IllegalArgumentException if the platform type is not supported
     */
    public static DnsBlockerHandler createHandler(DnsBlockerConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("DnsBlockerConfig cannot be null");
        }

        DnsBlockerType platform = config.platform();
        if (platform == null) {
            // Default to Pi-hole for backward compatibility
            platform = DnsBlockerType.PIHOLE;
        }

        return switch (platform) {
            case PIHOLE -> new PiHoleHandler(config);
            case ADGUARD_HOME -> new AdGuardHomeHandler(config);
        };
    }

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private DnsBlockerHandlerFactory() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}

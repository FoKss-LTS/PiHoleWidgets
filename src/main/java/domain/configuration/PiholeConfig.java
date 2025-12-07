/*
 *
 *  Copyright (C) 2022.  Reda ELFARISSI aka foxy999
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

package domain.configuration;

public class PiholeConfig {

    private String IPAddress;
    private String AUTH;
    private int Port;
    private String Scheme;


    public PiholeConfig(String IPAddress, int Port, String Scheme, String AUTH) {
        this.IPAddress = IPAddress;
        this.AUTH = AUTH;
        this.Port = Port;
        this.Scheme = Scheme;
    }

    public String getIPAddress() {
        return IPAddress;
    }

    public String getAUTH() {
        return AUTH;
    }

    public int getPort() {
        return Port;
    }

    public String getScheme() {
        return Scheme;
    }
}

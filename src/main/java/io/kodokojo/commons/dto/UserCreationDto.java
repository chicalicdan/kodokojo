/**
 * Kodo Kojo - Software factory done right
 * Copyright © 2017 Kodo Kojo (infos@kodokojo.io)
 *
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.kodokojo.commons.dto;



import io.kodokojo.commons.model.User;

import java.util.Set;

import static org.apache.commons.lang.StringUtils.isBlank;

public class UserCreationDto extends UserDto {

    private final String privateKey;


    private final String password;

    public UserCreationDto(User user, String privateKey) {
        super(user);
        if (isBlank(privateKey)) {
            throw new IllegalArgumentException("privateKey must be defined.");
        }
        this.privateKey = privateKey;
        this.password = user.getPassword();
    }
    public String getPrivateKey() {
        return privateKey;
    }
}

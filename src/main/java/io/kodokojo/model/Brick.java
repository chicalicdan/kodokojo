package io.kodokojo.model;

/*
 * #%L
 * kodokojo-commons
 * %%
 * Copyright (C) 2016 Kodo-kojo
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import io.kodokojo.project.starter.ProjectConfigurer;

import static org.apache.commons.lang.StringUtils.isBlank;

public class Brick {

    private final String name;

    private final BrickType type;

    private final ProjectConfigurer configurer;

    public Brick(String name, BrickType type, ProjectConfigurer configurer) {
        if (isBlank(name)) {
            throw new IllegalArgumentException("name must be defined.");
        }
        if (type == null) {
            throw new IllegalArgumentException("type must be defined.");
        }
        this.name = name;
        this.type = type;
        this.configurer = configurer;
    }

    public String getName() {
        return name;
    }

    public BrickType getType() {
        return type;
    }

    public ProjectConfigurer getConfigurer() {
        return configurer;
    }

    @Override
    public String toString() {
        return "Brick{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", configurer=" + configurer +
                '}';
    }
}

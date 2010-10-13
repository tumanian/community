/**
 * Copyright (c) 2002-2010 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.neo4j.index.impl.lucene;

public class ValueContext
{
    final Object value;
    boolean indexNumeric;

    public ValueContext( Object value )
    {
        this.value = value;
    }
    
    public ValueContext indexNumeric()
    {
        if ( !( this.value instanceof Number ) )
        {
            throw new IllegalStateException( "Value should be a Number, is " + value +
                    " (" + value.getClass() + ")" );
        }
        this.indexNumeric = true;
        return this;
    }
    
    Object getCorrectValue()
    {
        return this.indexNumeric ? this.value : this.value.toString();
    }
    
    @Override
    public String toString()
    {
        return value.toString();
    }
}

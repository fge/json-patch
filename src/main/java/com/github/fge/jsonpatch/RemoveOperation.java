/*
 * Copyright (c) 2014, Francis Galiegue (fgaliegue@gmail.com)
 *
 * This software is dual-licensed under:
 *
 * - the Lesser General Public License (LGPL) version 3.0 or, at your option, any
 *   later version;
 * - the Apache Software License (ASL) version 2.0.
 *
 * The text of this file and of both licenses is available at the root of this
 * project or, if you have the jar distribution, in directory META-INF/, under
 * the names LGPL-3.0.txt and ASL-2.0.txt respectively.
 *
 * Direct link to the sources:
 *
 * - LGPL 3.0: https://www.gnu.org/licenses/lgpl-3.0.txt
 * - ASL 2.0: http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package com.github.fge.jsonpatch;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jackson.jsonpointer.TokenResolver;
import com.google.common.collect.Iterables;


import java.io.IOException;

/**
 * JSON Path {@code remove} operation
 *
 * <p>This operation only takes one pointer ({@code path}) as an argument. It
 * is an error condition if no JSON value exists at that pointer.</p>
 */
public final class RemoveOperation
    extends JsonPatchOperation
{
    @JsonCreator
    public RemoveOperation(@JsonProperty("path") final JsonPointer path)
    {
        super("remove", path);
    }

    public JsonNode apply(JsonNode node) throws JsonPatchException {
        if (this.path.isEmpty()) {
            return MissingNode.getInstance();
            /**
             * since we are overriding the Remove Operation for allowing remove by array element,
             * we need to skip this check if the parent is an array, as otherwise the library will try to get node by index but the value is supplied
             */

        } else if (this.path.parent() != null && !((JsonNode)this.path.parent().get(node)).isArray() && ((JsonNode)this.path.path(node)).isMissingNode()) {
            throw new JsonPatchException(BUNDLE.getMessage("jsonPatch.noSuchPath"));
        } else {
            JsonNode ret = node.deepCopy();
            JsonNode parentNode = (JsonNode)this.path.parent().get(ret);
            String raw = ((TokenResolver)Iterables.getLast(this.path)).getToken().getRaw();
            if (parentNode.isObject()) {
                ((ObjectNode)parentNode).remove(raw);
            } else {
                //loop through array to remove only specified element
                for(int i = 0; i < parentNode.size(); ++i) {
                    if (parentNode.get(i).asLong() == Long.valueOf(raw)) {
                        ((ArrayNode)parentNode).remove(i);
                    }
                }
            }

            return ret;
        }
    }

    @Override
    public void serialize(final JsonGenerator jgen,
        final SerializerProvider provider)
        throws IOException, JsonProcessingException
    {
        jgen.writeStartObject();
        jgen.writeStringField("op", "remove");
        jgen.writeStringField("path", path.toString());
        jgen.writeEndObject();
    }

    @Override
    public void serializeWithType(final JsonGenerator jgen,
        final SerializerProvider provider, final TypeSerializer typeSer)
        throws IOException, JsonProcessingException
    {
        serialize(jgen, provider);
    }

    @Override
    public String toString()
    {
        return "op: " + op + "; path: \"" + path + '"';
    }
}

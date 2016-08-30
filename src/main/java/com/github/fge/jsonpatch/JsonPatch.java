/*
 * Copyright (c) 2014, Francis Galiegue (fgaliegue@gmail.com)
 * Copyright (c) 2016, Alexander Patrikalakis (amcp@me.com)
 * Copyright (c) 2015, Daisuke Miyamoto (dai.0304@gmail.com)
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

import com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.github.fge.jackson.JacksonUtils;
import com.github.fge.msgsimple.bundle.MessageBundle;
import com.github.fge.msgsimple.load.MessageBundles;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.List;

/**
 * Implementation of JSON Patch
 *
 * <p><a href="http://tools.ietf.org/html/draft-ietf-appsawg-json-patch-10">JSON
 * Patch</a>, as its name implies, is an IETF draft describing a mechanism to
 * apply a patch to any JSON value. This implementation covers all operations
 * according to the specification; however, there are some subtle differences
 * with regards to some operations which are covered in these operations'
 * respective documentation.</p>
 *
 * <p>An example of a JSON Patch is as follows:</p>
 *
 * <pre>
 *     [
 *         {
 *             "op": "add",
 *             "path": "/-",
 *             "value": {
 *                 "productId": 19,
 *                 "name": "Duvel",
 *                 "type": "beer"
 *             }
 *         }
 *     ]
 * </pre>
 *
 * <p>This patch contains a single operation which adds an item at the end of
 * an array. A JSON Patch can contain more than one operation; in this case, all
 * operations are applied to the input JSON value in their order of appearance,
 * until all operations are applied or an error condition is encountered.</p>
 *
 * <p>The main point where this implementation differs from the specification
 * is initial JSON parsing. The draft says:</p>
 *
 * <pre>
 *     Operation objects MUST have exactly one "op" member
 * </pre>
 *
 * <p>and:</p>
 *
 * <pre>
 *     Additionally, operation objects MUST have exactly one "path" member.
 * </pre>
 *
 * <p>However, obeying these to the letter forces constraints on the JSON
 * <b>parser</b>. Here, these constraints are not enforced, which means:</p>
 *
 * <pre>
 *     [ { "op": "add", "op": "remove", "path": "/x" } ]
 * </pre>
 *
 * <p>is parsed (as a {@code remove} operation, since it appears last).</p>
 *
 * <p><b>IMPORTANT NOTE:</b> the JSON Patch is supposed to be VALID when the
 * constructor for this class ({@link JsonPatch#fromJson(JsonNode)} is used.</p>
 */
public class JsonPatch
    implements JsonSerializable, Supplier<ExpressionSpecBuilder>
{
    private static final MessageBundle BUNDLE
        = MessageBundles.getBundle(JsonPatchMessages.class);

    /**
     * List of operations
     */
    protected final List<JsonPatchOperation> operations;

    /**
     * Constructor
     *
     * <p>Normally, you should never have to use it.</p>
     *
     * @param operations the list of operations for this patch
     * @see JsonPatchOperation
     */
    @JsonCreator
    public JsonPatch(final List<JsonPatchOperation> operations)
    {
        this.operations = ImmutableList.copyOf(operations);
    }

    /**
     * Static factory method to build a JSON Patch out of a JSON representation
     *
     * @param node the JSON representation of the generated JSON Patch
     * @return a JSON Patch
     * @throws IOException input is not a valid JSON patch
     * @throws NullPointerException input is null
     */
    public static JsonPatch fromJson(final JsonNode node)
        throws IOException
    {
        BUNDLE.checkNotNull(node, "jsonPatch.nullInput");
        return JacksonUtils.getReader().forType(JsonPatch.class)
            .readValue(node);
    }

    /**
     * Apply this patch to a JSON value
     *
     * @param node the value to apply the patch to
     * @return the patched JSON value
     * @throws JsonPatchException failed to apply patch
     * @throws NullPointerException input is null
     */
    public JsonNode apply(final JsonNode node)
        throws JsonPatchException
    {
        BUNDLE.checkNotNull(node, "jsonPatch.nullInput");
        JsonNode ret = node;
        for (final JsonPatchOperation operation: operations)
            ret = operation.apply(ret);

        return ret;
    }
    
    /**
     * Converts this JsonPatch into an ExpressionSpecBuilder
     * @return an expression spec builder that contains the updates contained in this
     * patch
     */
	public ExpressionSpecBuilder get() {
		ExpressionSpecBuilder builder = new ExpressionSpecBuilder();
		for(JsonPatchOperation operation : operations) {
			operation.applyToBuilder(builder);
		}
		return builder;
	}

    @Override
    public String toString()
    {
        return operations.toString();
    }

    @Override
    public void serialize(final JsonGenerator jgen,
        final SerializerProvider provider)
        throws IOException
    {
        jgen.writeStartArray();
        for (final JsonPatchOperation op: operations)
            op.serialize(jgen, provider);
        jgen.writeEndArray();
    }

    @Override
    public void serializeWithType(final JsonGenerator jgen,
        final SerializerProvider provider, final TypeSerializer typeSer)
        throws IOException
    {
        serialize(jgen, provider);
    }
}

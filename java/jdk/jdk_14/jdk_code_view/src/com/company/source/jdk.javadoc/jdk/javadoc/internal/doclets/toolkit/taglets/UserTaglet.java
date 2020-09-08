/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package jdk.javadoc.internal.doclets.toolkit.taglets;

import java.util.Collections;
import java.util.List;

import javax.lang.model.element.Element;

import com.sun.source.doctree.DocTree;
import jdk.javadoc.internal.doclets.formats.html.markup.RawHtml;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.util.Utils;

import static jdk.javadoc.doclet.Taglet.Location.*;

/**
 * A taglet wrapper, allows the public taglet {@link jdk.javadoc.doclet.Taglet}
 * wrapped into an internal Taglet representation.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class UserTaglet implements Taglet {

    final private jdk.javadoc.doclet.Taglet userTaglet;

    public UserTaglet(jdk.javadoc.doclet.Taglet t) {
        userTaglet = t;
    }

    /**
     * {@inheritDoc}
     */
    public boolean inField() {
        return userTaglet.isInlineTag()
                || userTaglet.getAllowedLocations().contains(FIELD);
    }

    /**
     * {@inheritDoc}
     */
    public boolean inConstructor() {
        return userTaglet.isInlineTag()
                || userTaglet.getAllowedLocations().contains(CONSTRUCTOR);
    }

    /**
     * {@inheritDoc}
     */
    public boolean inMethod() {
        return userTaglet.isInlineTag()
                || userTaglet.getAllowedLocations().contains(METHOD);
    }

    /**
     * {@inheritDoc}
     */
    public boolean inOverview() {
        return userTaglet.isInlineTag()
                || userTaglet.getAllowedLocations().contains(OVERVIEW);
    }

    /**
     * {@inheritDoc}
     */
    public boolean inModule() {
        return userTaglet.isInlineTag()
                || userTaglet.getAllowedLocations().contains(MODULE);
    }

    /**
     * {@inheritDoc}
     */
    public boolean inPackage() {
        return userTaglet.isInlineTag()
                || userTaglet.getAllowedLocations().contains(PACKAGE);
    }

    /**
     * {@inheritDoc}
     */
    public boolean inType() {
        return userTaglet.isInlineTag()
                || userTaglet.getAllowedLocations().contains(TYPE);
    }

    /**
     * Return true if this <code>Taglet</code> is an inline tag.
     *
     * @return true if this <code>Taglet</code> is an inline tag and false otherwise.
     */
    public boolean isInlineTag() {
        return userTaglet.isInlineTag();
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return userTaglet.getName();
    }

    /**
     * {@inheritDoc}
     */
    public Content getTagletOutput(Element element, DocTree tag, TagletWriter writer){
        Content output = writer.getOutputInstance();
        output.add(new RawHtml(userTaglet.toString(Collections.singletonList(tag), element)));
        return output;
    }

    /**
     * {@inheritDoc}
     */
    public Content getTagletOutput(Element holder, TagletWriter writer) {
        Content output = writer.getOutputInstance();
        Utils utils = writer.configuration().utils;
        List<? extends DocTree> tags = utils.getBlockTags(holder, getName());
        if (!tags.isEmpty()) {
            String tagString = userTaglet.toString(tags, holder);
            if (tagString != null) {
                output.add(new RawHtml(tagString));
            }
        }
        return output;
    }
}

/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.util.locale.provider;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.spi.LocaleServiceProvider;

/**
 * LocaleProviderAdapter implementation for the host locale data.
 * Currently it is only implemented on Windows Vista or later.
 *
 * @author Naoto Sato
 */
public class HostLocaleProviderAdapter extends AuxLocaleProviderAdapter {

    /**
     * Returns the type of this LocaleProviderAdapter
     */
    @Override
    public LocaleProviderAdapter.Type getAdapterType() {
        return LocaleProviderAdapter.Type.HOST;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <P extends LocaleServiceProvider> P findInstalledProvider(final Class<P> c) {
        try {
            Method getter = HostLocaleProviderAdapterImpl.class.getMethod(
                    "get" + c.getSimpleName(), (Class<?>[]) null);
            return (P)getter.invoke(null, (Object[]) null);
        }  catch (NoSuchMethodException |
                  IllegalAccessException |
                  IllegalArgumentException |
                  InvocationTargetException ex) {
            LocaleServiceProviderPool.config(HostLocaleProviderAdapter.class, ex.toString());
        }
        return null;
    }

    /**
     * Utility to make the decimal format specific to integer, called
     * by the platform dependent adapter implementations.
     *
     * @param df A DecimalFormat object
     * @return The same DecimalFormat object in the argument, modified
     *          to allow integer formatting/parsing only.
     */
    static DecimalFormat makeIntegerFormatter(DecimalFormat df) {
        df.setMaximumFractionDigits(0);
        df.setDecimalSeparatorAlwaysShown(false);
        df.setParseIntegerOnly(true);
        return df;
    }
}

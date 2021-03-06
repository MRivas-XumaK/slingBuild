/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.resourcemerger.impl;

import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceProviderFactory;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.resourcemerger.api.ResourceMergerService;

@Component(label = "Apache Sling Merged Resource Provider Factory",
           description = "This resource provider delivers merged resources based on the search paths.",
           metatype=true)
@Service(value = {ResourceProviderFactory.class, ResourceMergerService.class})
@Properties({
    @Property(name = ResourceProvider.ROOTS, value=MergedResourceProviderFactory.DEFAULT_ROOT,
            label="Root",
            description="The mount point of merged resources"),
    @Property(name = ResourceProvider.OWNS_ROOTS, boolValue=true, propertyPrivate=true)
})
/**
 * The <code>MergedResourceProviderFactory</code> creates merged resource
 * providers and implements the <code>ResourceMergerService</code>.
 */
public class MergedResourceProviderFactory implements ResourceProviderFactory, ResourceMergerService {

    public static final String DEFAULT_ROOT = "/mnt/overlay";

    private String mergeRootPath;

    /**
     * {@inheritDoc}
     */
    public ResourceProvider getResourceProvider(final Map<String, Object> stringObjectMap)
    throws LoginException {
        return new MergedResourceProvider(mergeRootPath);
    }

    /**
     * {@inheritDoc}
     */
    public ResourceProvider getAdministrativeResourceProvider(final Map<String, Object> stringObjectMap)
    throws LoginException {
        return new MergedResourceProvider(mergeRootPath);
    }

    /**
     * {@inheritDoc}
     */
    public String getMergedResourcePath(final String relativePath) {
        if (relativePath == null) {
            throw new IllegalArgumentException("Provided relative path is null");
        }

        if (relativePath.startsWith("/")) {
            throw new IllegalArgumentException("Provided path is not a relative path");
        }

        return mergeRootPath + "/" + relativePath;
    }

    /**
     * {@inheritDoc}
     */
    public Resource getMergedResource(final Resource resource) {
        if (resource != null) {
            final ResourceResolver resolver = resource.getResourceResolver();
            final String[] searchPaths = resolver.getSearchPath();
            for (final String searchPathPrefix : searchPaths) {
                if (resource.getPath().startsWith(searchPathPrefix)) {
                    final String searchPath = searchPathPrefix.substring(0, searchPathPrefix.length() - 1);
                    return resolver.getResource(resource.getPath().replaceFirst(searchPath, mergeRootPath));
                }
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMergedResource(final Resource resource) {
        if (resource == null) {
            return false;
        }

        return Boolean.TRUE.equals(resource.getResourceMetadata().get(MergedResourceConstants.METADATA_FLAG));
    }

    /**
     * {@inheritDoc}
     */
    public String getResourcePath(final String searchPath, final String mergedResourcePath) {
        if( searchPath == null || !searchPath.startsWith("/") || !searchPath.endsWith("/") ) {
            throw new IllegalArgumentException("Provided path is not a valid search path: " + searchPath);
        }
        if ( mergedResourcePath == null || !mergedResourcePath.startsWith(this.mergeRootPath + "/") ) {
            throw new IllegalArgumentException("Provided path does not point to a merged resource: " + mergedResourcePath);
        }
        return searchPath + mergedResourcePath.substring(this.mergeRootPath.length() + 1);
    }

    @Activate
    protected void configure(final Map<String, Object> properties) {
        mergeRootPath = PropertiesUtil.toString(properties.get(ResourceProvider.ROOTS), DEFAULT_ROOT);
        if (mergeRootPath.endsWith("/")) {
            mergeRootPath = mergeRootPath.substring(0, mergeRootPath.length() - 1);
        }
    }
}

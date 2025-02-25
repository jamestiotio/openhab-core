/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.core.internal.items;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.AbstractManagedProvider;
import org.openhab.core.items.ManagedMetadataProvider;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataPredicates;
import org.openhab.core.items.MetadataProvider;
import org.openhab.core.storage.StorageService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ManagedMetadataProviderImpl} is an OSGi service, that allows to add or remove
 * metadata for items at runtime. Persistence of added metadata is handled by
 * a {@link StorageService}.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { MetadataProvider.class, ManagedMetadataProvider.class })
public class ManagedMetadataProviderImpl extends AbstractManagedProvider<Metadata, MetadataKey, Metadata>
        implements ManagedMetadataProvider {

    private final Logger logger = LoggerFactory.getLogger(ManagedMetadataProviderImpl.class);

    @Activate
    public ManagedMetadataProviderImpl(final @Reference StorageService storageService) {
        super(storageService);
    }

    @Override
    protected String getStorageName() {
        return Metadata.class.getName();
    }

    @Override
    protected String keyToString(MetadataKey key) {
        return key.toString();
    }

    @Override
    protected @Nullable Metadata toElement(String key, Metadata persistableElement) {
        return persistableElement;
    }

    @Override
    protected Metadata toPersistableElement(Metadata element) {
        return element;
    }

    /**
     * Removes all metadata of a given item
     *
     * @param itemname the name of the item for which the metadata is to be removed.
     */
    @Override
    public void removeItemMetadata(String name) {
        logger.debug("Removing all metadata for item {}", name);
        getAll().stream().filter(MetadataPredicates.ofItem(name)).map(Metadata::getUID).forEach(this::remove);
    }

    @Override
    public Collection<Metadata> getAll() {
        return super.getAll().stream().map(this::normalizeMetadata).collect(Collectors.toUnmodifiableList());
    }

    @Override
    public @Nullable Metadata get(MetadataKey key) {
        Metadata metadata = super.get(key);
        if (metadata != null) {
            return normalizeMetadata(metadata);
        }
        return null;
    }

    private Metadata normalizeMetadata(Metadata metadata) {
        return new Metadata(metadata.getUID(), metadata.getValue(), metadata.getConfiguration().entrySet().stream()
                .map(this::normalizeConfigEntry).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    private Map.Entry<String, Object> normalizeConfigEntry(Map.Entry<String, Object> entry) {
        Object value = entry.getValue();
        if (value instanceof Integer) {
            BigDecimal newValue = new BigDecimal(value.toString());
            newValue.setScale(0);
            return Map.entry(entry.getKey(), newValue);
        } else if (value instanceof Number) {
            return Map.entry(entry.getKey(), new BigDecimal(value.toString()));
        }

        return entry;
    }
}

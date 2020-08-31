package com.p4ybill.engine.index;

import com.p4ybill.engine.store.SimpleFileSerializer;

import java.util.Map;

abstract class EngineMetaAbstract<T> {

    public enum EngineMeta {
        TOTAL_DOCS_KEY,
        TOTAL_TERMS_KEY
    }

    protected SimpleFileSerializer<Map<EngineMeta, T>> serializer;
    protected Map<EngineMeta, T> metaDataMap;
}

package potemkin;

import clojure.lang.ILookup;
import clojure.lang.MapEntry;

public class LazyMapEntry extends clojure.lang.MapEntry {
    private final Object _key;
    private final ILookup _map;

    public LazyMapEntry(ILookup map, Object key) {
        super(key, null);
        _key = key;
        _map = map;
    }

    @Override
    public Object val() {
        return _map.valAt(_key);
    }
}

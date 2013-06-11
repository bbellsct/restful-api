/* ***************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.extractors

/**
 * Class to provide abilities to transform maps by renaming keys and
 * providing default values.
 * The MapTransformer operates on nested maps that may also contain collections.
 * The keys of the map(s) are assumed to be strings.
 * A path is a sequence of keys denoting a key within the map or a sub-map.
 * If any part of a path denotes a key whose value is a collection, then the rest of
 * the path is applied to all elements of the collection.
 * For example, if the transformer is configured to rename the path ['books','title'] to
 * 'bookTitle' and the value of 'books' is a Set of maps representing the books, then each
 * map in the books set that contains the key 'title' will have it renamed to 'myTitle'.
 *
 * All rules should be specified in terms of paths within the original map.
 * Rules are applied depth-first.
 **/
class MapTransformer {

    //map of paths (lists of string) to arrays of closures
    //that define transformations to apply to that path
    def actions = [:]

    def renameRules = [:]
    def flattenPaths = []

    private boolean initialized = false

    /**
     * Renames the final element of the path to a new
     * value.
     * @param path a list of strings denoting the path to rename
     * @param newKey the new name for the last element of the path
     */
    void addRenameRule(List<String> path, String newKey) {
        renameRules.put(path, newKey)

    }

    /**
     * Adds a default value for the specified path
     * if the key denoted by the path is not already present.
     * @param path a list of strings denoting the path to rename
     * @param newKey the new name for the last element of the path
     **/
    void addDefaultValueRule(List<String> path, def defaultValue) {
        path = path.clone()
        def localKey = path.pop()
        Closure c = {Map m ->
            if (!m.containsKey(localKey)) {
                m.put(localKey, defaultValue)
            }
        }
        addDefaultValueClosure(path, c)
    }

    void addModifyValueRule(List<String> path, Closure closure) {
        path = path.clone()
        def localKey = path.pop()
        Closure c = {Map m ->
            if (m.containsKey(localKey)) {
                def val = m.get(localKey)
                val = closure.call(val)
                m.put(localKey, val)
            }
        }
        addModifyValueClosure(path, c)
    }

    void addFlattenRule(List<String> path) {
        if (!flattenPaths.contains(path)) {
            flattenPaths.add path
        }
    }

    /**
     * Transforms the specified map.
     * @param map the map to transform
     **/
    def transform(Map map) {
        init()
        def keys = actions.keySet()
        //sort longest to shortest paths
        keys = keys.sort { a,b -> b.size() <=> a.size() }

        keys.each { path ->
            path = path.clone()
            transform(map, [], path)
        }
        map
    }

    /**
     * Transforms the a map by apply closures to the path.
     **/
    protected transform(Map currentObject, def objectPath, def remainingPath) {
        if (remainingPath.size() == 0) {
            def closureHolder = actions.get(objectPath)

            closureHolder.defaultValueClosures.each { Closure c ->
                c.call(currentObject)
            }
            closureHolder.modifyValueClosures.each { Closure c ->
                c.call(currentObject)
            }
            closureHolder.renameClosures.each { Closure c ->
                c.call(currentObject)
            }
            closureHolder.flattenClosures.each { Closure c ->
                c.call(currentObject)
            }
        } else {
            def key = remainingPath.remove(0)
            objectPath.add key
            if (currentObject.containsKey(key)) {
                def nextObject = currentObject[key]
                if (nextObject instanceof Map) {
                    transform(nextObject, objectPath, remainingPath)
                } else if (nextObject instanceof List) {
                    nextObject.each {
                        if (it instanceof Map) {
                            transform(it, objectPath, remainingPath)
                        }
                    }
                }
            }
            remainingPath.add objectPath.pop()
        }

    }

    protected void init() {
        if (!initialized) {
            renameRules.entrySet().each { Map.Entry entry ->
                def path = entry.key
                def newKey = entry.value
                path = path.clone()
                def oldKey = path.pop()
                Closure c = {Map m ->
                    if (m.containsKey(oldKey)) {
                        def val = m.remove(oldKey)
                        m.put(newKey, val)
                    }
                }
                addRenameClosure(path, c)
            }

            flattenPaths.each { def path ->
                def newKey = renameRules.get(path)
                if (newKey != null) {
                    path.pop()
                    path.add newKey
                }
                path = path.clone()
                def localKey = path.pop()
                Closure c = {Map m ->
                    if (m.containsKey(localKey)) {
                        def val = m.get(localKey)
                        if (val instanceof Collection) {
                            m.remove(localKey)
                            int index = 0
                            val.each {
                                if (it instanceof Map) {
                                    flattenInto(m, it, "$localKey[$index]")
                                    index++
                                }
                            }
                        } else {
                            if (val instanceof Map) {
                                m.remove(localKey)
                                flattenInto(m, val, localKey)
                            }
                        }
                    }
                }
                addFlattenClosure(path, c)
            }
        }
        initialized = true
    }

    private void addRenameClosure(List<String> path, Closure c) {
        getClosureHolder(path).renameClosures.add c
    }

    private void addDefaultValueClosure(List<String> path, Closure c) {
        getClosureHolder(path).defaultValueClosures.add c
    }

    private void addModifyValueClosure(List<String> path, Closure c) {
        getClosureHolder(path).modifyValueClosures.add c
    }

    private void addFlattenClosure(List<String> path, Closure c) {
        getClosureHolder(path).flattenClosures.add c
    }

    private ClosureHolder getClosureHolder(List<String> path) {
        if (!actions.containsKey(path)) {
            actions[path] = new ClosureHolder()
        }
        actions[path]
    }

    /**
     * Flattens the source map into the target map using the specified prefix.
     * All key/value pairs in the source map are added to the target as
     * "prefix.key"/value pairs.
     **/
    private void flattenInto(Map target, Map source, String prefix) {
        source.entrySet().each { Map.Entry entry ->
            target.put(prefix + "." + entry.key, entry.value)
        }
        target
    }

    class ClosureHolder {
        def renameClosures = []
        def defaultValueClosures = []
        def modifyValueClosures = []
        def flattenClosures = []
    }
}
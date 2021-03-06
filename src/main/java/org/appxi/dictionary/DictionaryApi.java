package org.appxi.dictionary;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class DictionaryApi {
    private static DictionaryApi defaultApi;

    public static void setupDefaultApi(Path repo) {
        defaultApi = new DictionaryApi(repo);
    }

    public static DictionaryApi api() {
        return defaultApi;
    }

    //

    private final Map<String, DictionaryEx> dictionaries = new LinkedHashMap<>();

    public final Path repo;

    public DictionaryApi(Path repo) {
        this.repo = repo;
        //
        try {
            Files.walkFileTree(repo, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if ("dict.properties".equals(file.getFileName().toString())) {
                        DictionaryEx dictionary = new DictionaryEx(file);
                        if (!dictionaries.containsKey(dictionary.id)) {
                            dictionaries.put(dictionary.id, dictionary);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Dictionary getDictionary(String id) {
        return this.dictionaries.get(id);
    }

    public DictionaryModel getDictionaryModel(String id) {
        return this.dictionaries.get(id).model;
    }

    public List<Dictionary> listDictionaries() {
        return this.dictionaries.values().stream().map(v -> (Dictionary) v).toList();
    }

    public List<DictionaryModel> listDictionaryModels() {
        return this.dictionaries.values().stream().map(v -> v.model).toList();
    }

    public void forEach(Consumer<Dictionary> consumer) {
        this.dictionaries.values().forEach(consumer);
    }

    public void forEach(BiConsumer<Dictionary, DictionaryModel> consumer) {
        this.dictionaries.values().forEach(v -> consumer.accept(v, v.model));
    }

    /**
     * ???????????????????????????????????????keywords??????:
     * <p>?????????????????????1???????????????????????? ??? ??????*???2??????????????????*?????????3??????????????????*??????*???4????????????????????????????????????"??????"???</p>
     *
     * @param keywords ?????????
     * @return ????????????
     */
    public Iterator<DictEntry.Scored> search(String keywords) {
        return search(keywords, null, null);
    }

    /**
     * ?????????????????????????????????????????????keywords??????:
     * <p>?????????????????????1???????????????????????? ??? ??????*???2??????????????????*?????????3??????????????????*??????*???4????????????????????????????????????"??????"???</p>
     *
     * @param keywords         ?????????
     * @param dictionaryFilter ???????????????
     * @return ????????????
     */
    public Iterator<DictEntry.Scored> search(String keywords,
                                             Predicate<Dictionary> dictionaryFilter) {
        return search(keywords, null, dictionaryFilter);
    }

    /**
     * ???????????????????????????????????????????????????keywords??????:
     * <p>?????????????????????1???????????????????????? ??? ??????*???2??????????????????*?????????3??????????????????*??????*???4????????????????????????????????????"??????"???</p>
     *
     * @param keywords         ?????????
     * @param entryFilter      ???????????????
     * @param dictionaryFilter ???????????????
     * @return ????????????
     */
    public Iterator<DictEntry.Scored> search(String keywords,
                                             Predicate<DictEntry> entryFilter,
                                             Predicate<Dictionary> dictionaryFilter) {
        final StringBuilder buf = new StringBuilder(null == keywords ? "" : keywords);
        final DictEntryExpr entryExpr = DictEntryExpr.detectForTitle(buf);
        return search(buf.toString(), entryExpr, entryFilter, dictionaryFilter);
    }

    /**
     * ?????????????????????????????????????????????????????????????????????
     *
     * @param keywords         ?????????
     * @param entryExpr        ????????????
     * @param entryFilter      ???????????????
     * @param dictionaryFilter ???????????????
     * @return ????????????
     */
    public Iterator<DictEntry.Scored> search(String keywords,
                                             DictEntryExpr entryExpr,
                                             Predicate<DictEntry> entryFilter,
                                             Predicate<Dictionary> dictionaryFilter) {
        return new SearcherIterator(this.listDictionaryModels(), keywords, entryExpr, entryFilter, dictionaryFilter);
    }

    //

    private static class DictionaryEx extends Dictionary {
        /**
         * ????????????????????????????????????
         */
        private final DictionaryModel model;

        public DictionaryEx(Path confFile) {
            super(confFile);
            model = new DictionaryModel(this);
        }
    }

    private static class SearcherIterator implements Iterator<DictEntry.Scored> {
        private final Iterator<DictionaryModel> modelsIterator;

        private final String keywords;
        private final DictEntryExpr entryExpr;
        private final Predicate<DictEntry> entryFilter;
        private final Predicate<Dictionary> dictionaryFilter;
        private Iterator<DictEntry.Scored> nextIterator;
        private DictEntry.Scored nextEntry;

        public SearcherIterator(Collection<DictionaryModel> dictionaryModels,
                                String keywords,
                                DictEntryExpr entryExpr,
                                Predicate<DictEntry> entryFilter,
                                Predicate<Dictionary> dictionaryFilter) {
            this.keywords = keywords;
            this.entryExpr = entryExpr;
            this.entryFilter = entryFilter;

            this.modelsIterator = dictionaryModels.iterator();
            this.dictionaryFilter = dictionaryFilter;
        }

        @Override
        public boolean hasNext() {
            if (null == this.nextIterator) {
                if (!this.modelsIterator.hasNext()) {
                    return false;
                }
                DictionaryModel nextModel = this.modelsIterator.next();
                if (null != dictionaryFilter && !dictionaryFilter.test(nextModel.dictionary))
                    return hasNext();
                this.nextIterator = nextModel.search(keywords, entryExpr, entryFilter);
            }

            if (!this.nextIterator.hasNext()) {
                this.nextIterator = null;
                return hasNext();
            }
            this.nextEntry = this.nextIterator.next();
            return true;
        }

        @Override
        public DictEntry.Scored next() {
            return this.nextEntry;
        }
    }
}

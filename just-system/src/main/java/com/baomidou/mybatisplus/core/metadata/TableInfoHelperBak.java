///*
// * Copyright (c) 2011-2020, baomidou (jobob@qq.com).
// * <p>
// * Licensed under the Apache License, Version 2.0 (the "License"); you may not
// * use this file except in compliance with the License. You may obtain a copy of
// * the License at
// * <p>
// * https://www.apache.org/licenses/LICENSE-2.0
// * <p>
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// * License for the specific language governing permissions and limitations under
// * the License.
// */
//package com.baomidou.mybatisplus.core.metadata;
//
//import com.baomidou.mybatisplus.annotation.*;
//import com.baomidou.mybatisplus.annotation.impl.TableFieldImp;
//import com.baomidou.mybatisplus.annotation.impl.TableIdImp;
//import com.baomidou.mybatisplus.core.MybatisPlusVersion;
//import com.baomidou.mybatisplus.core.config.GlobalConfig;
//import com.baomidou.mybatisplus.core.incrementer.IKeyGenerator;
//import com.baomidou.mybatisplus.core.toolkit.*;
//import org.apache.ibatis.builder.MapperBuilderAssistant;
//import org.apache.ibatis.builder.StaticSqlSource;
//import org.apache.ibatis.executor.keygen.KeyGenerator;
//import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
//import org.apache.ibatis.logging.Log;
//import org.apache.ibatis.logging.LogFactory;
//import org.apache.ibatis.mapping.MappedStatement;
//import org.apache.ibatis.mapping.ResultMap;
//import org.apache.ibatis.mapping.SqlCommandType;
//import org.apache.ibatis.reflection.Reflector;
//import org.apache.ibatis.reflection.ReflectorFactory;
//import org.apache.ibatis.session.Configuration;
//
//import javax.persistence.*;
//import java.beans.PropertyDescriptor;
//import java.lang.reflect.Field;
//import java.lang.reflect.InvocationTargetException;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//
//import static java.util.stream.Collectors.toList;
//
///**
// * <p>
// * ???????????????????????????
// * </p>
// *
// * @author hubin sjy
// * @since 2016-09-09
// */
//public class TableInfoHelperBak {
//
//    private static final Log logger = LogFactory.getLog(TableInfoHelper.class);
//
//    /**
//     * ????????????????????????
//     */
//    private static final Map<Class<?>, TableInfo> TABLE_INFO_CACHE = new ConcurrentHashMap<>();
//
//    /**
//     * ?????????????????????
//     */
//    private static final String DEFAULT_ID_NAME = "id";
//
//    /**
//     * <p>
//     * ???????????????????????????
//     * </p>
//     *
//     * @param clazz ???????????????
//     * @return ????????????????????????
//     */
//    public static TableInfo getTableInfo(Class<?> clazz) {
//        if (clazz == null
//                || ReflectionKit.isPrimitiveOrWrapper(clazz)
//                || clazz == String.class) {
//            return null;
//        }
//        // https://github.com/baomidou/mybatis-plus/issues/299
//        TableInfo tableInfo = TABLE_INFO_CACHE.get(ClassUtils.getUserClass(clazz));
//        if (null != tableInfo) {
//            return tableInfo;
//        }
//        //????????????????????????
//        Class<?> currentClass = clazz;
//        while (null == tableInfo && Object.class != currentClass) {
//            currentClass = currentClass.getSuperclass();
//            tableInfo = TABLE_INFO_CACHE.get(ClassUtils.getUserClass(currentClass));
//        }
//        if (tableInfo != null) {
//            TABLE_INFO_CACHE.put(ClassUtils.getUserClass(clazz), tableInfo);
//        }
//        return tableInfo;
//    }
//
//    /**
//     * <p>
//     * ?????????????????????????????????
//     * </p>
//     *
//     * @return ??????????????????????????????
//     */
//    @SuppressWarnings("unused")
//    public static List<TableInfo> getTableInfos() {
//        return Collections.unmodifiableList(new ArrayList<>(TABLE_INFO_CACHE.values()));
//    }
//
//    /**
//     * <p>
//     * ?????????????????????????????????????????????
//     * </p>
//     *
//     * @param clazz ???????????????
//     * @return ????????????????????????
//     */
//    public synchronized static TableInfo initTableInfo(MapperBuilderAssistant builderAssistant, Class<?> clazz) {
//        return TABLE_INFO_CACHE.computeIfAbsent(clazz, (key) -> {
//            /* ???????????????????????????,???????????? */
//            TableInfo tableInfo = new TableInfo(key);
//            GlobalConfig globalConfig;
//            if (null != builderAssistant) {
//                tableInfo.setCurrentNamespace(builderAssistant.getCurrentNamespace());
//                tableInfo.setConfiguration(builderAssistant.getConfiguration());
//                globalConfig = GlobalConfigUtils.getGlobalConfig(builderAssistant.getConfiguration());
//            } else {
//                // ??????????????????
//                globalConfig = GlobalConfigUtils.defaults();
//            }
//            if (globalConfig.isBanner()) {
//                System.out.println("MyBatis Plus (Jpa Patch)");
//            }
//            /* ????????????????????? */
//            final String[] excludeProperty = initTableName(key, globalConfig, tableInfo);
//            globalConfig.setBanner(false);
//
//            List<String> excludePropertyList = excludeProperty != null && excludeProperty.length > 0 ? Arrays.asList(excludeProperty) : Collections.emptyList();
//            globalConfig.setBanner(false);
//
//            /* ????????????????????? */
//            initTableFields(key, globalConfig, tableInfo, excludePropertyList);
//            globalConfig.setBanner(false);
//
//            /* ?????? lambda */
//            LambdaUtils.installCache(tableInfo);
//
//            /* ???????????? resultMap */
//            tableInfo.initResultMapIfNeed();
//
//            return tableInfo;
//        });
//    }
//
//    /**
//     * <p>
//     * ????????? ??????????????????,??????,resultMap
//     * </p>
//     *
//     * @param clazz        ?????????
//     * @param globalConfig ????????????
//     * @param tableInfo    ????????????????????????
//     * @return ????????????????????????
//     */
//    private static String[] initTableName(Class<?> clazz, GlobalConfig globalConfig, TableInfo tableInfo) {
//        /* ????????????????????? */
//        GlobalConfig.DbConfig dbConfig = globalConfig.getDbConfig();
//        /**
//         * JPA ??????
//         */
//        final Table jpaTableAnnotation = clazz.getAnnotation(Table.class);
//        /**
//         * MP ??????
//         */
//        final TableName mpTableAnnotation = clazz.getAnnotation(TableName.class);
//
//        String tableName = clazz.getSimpleName();
//        String tablePrefix = dbConfig.getTablePrefix();
//        String schema = dbConfig.getSchema();
//        boolean tablePrefixEffect = true;
//        String[] excludeProperty = null;
//        /**
//         * JPA ??????????????????????????? MP ????????????
//         */
//        if (Objects.nonNull(jpaTableAnnotation)) {
//            if (StringUtils.isNotBlank(jpaTableAnnotation.name())) {
//                tableName = jpaTableAnnotation.name();
//            } else {
//                tableName = initTableNameWithDbConfig(tableName, dbConfig);
//            }
//            if (StringUtils.isNotBlank(jpaTableAnnotation.schema())) {
//                schema = jpaTableAnnotation.schema();
//            }
//        }
//        /**
//         * MP table ??????
//         */
//        if (mpTableAnnotation != null) {
//            if (StringUtils.isNotBlank(mpTableAnnotation.value())) {
//                tableName = mpTableAnnotation.value();
//                if (StringUtils.isNotBlank(tablePrefix) && !mpTableAnnotation.keepGlobalPrefix()) {
//                    tablePrefixEffect = false;
//                }
//            } else if (Objects.isNull(jpaTableAnnotation)) {
//                tableName = initTableNameWithDbConfig(tableName, dbConfig);
//            }
//            if (StringUtils.isNotBlank(mpTableAnnotation.schema())) {
//                schema = mpTableAnnotation.schema();
//            }
//            /* ?????????????????? */
//            if (StringUtils.isNotBlank(mpTableAnnotation.resultMap())) {
//                tableInfo.setResultMap(mpTableAnnotation.resultMap());
//            }
//            tableInfo.setAutoInitResultMap(mpTableAnnotation.autoResultMap());
//            excludeProperty = mpTableAnnotation.excludeProperty();
//        } else if (Objects.isNull(jpaTableAnnotation)) {
//            tableName = initTableNameWithDbConfig(tableName, dbConfig);
//        }
//
//        String targetTableName = tableName;
//        if (StringUtils.isNotBlank(tablePrefix) && tablePrefixEffect) {
//            targetTableName = tablePrefix + targetTableName;
//        }
//        if (StringUtils.isNotBlank(schema)) {
//            targetTableName = schema + StringPool.DOT + targetTableName;
//        }
//
//        tableInfo.setTableName(targetTableName);
//
//        /* ?????????????????? KEY ????????? */
//        if (null != dbConfig.getKeyGenerator()) {
//            tableInfo.setKeySequence(clazz.getAnnotation(KeySequence.class));
//        }
//        return excludeProperty;
//    }
//
//    /**
//     * ?????? DbConfig ????????? ??????
//     *
//     * @param className ??????
//     * @param dbConfig  DbConfig
//     * @return ??????
//     */
//    private static String initTableNameWithDbConfig(String className, GlobalConfig.DbConfig dbConfig) {
//        String tableName = className;
//        // ???????????????????????????
//        if (dbConfig.isTableUnderline()) {
//            tableName = StringUtils.camelToUnderline(tableName);
//        }
//        // ??????????????????
//        if (dbConfig.isCapitalMode()) {
//            tableName = tableName.toUpperCase();
//        } else {
//            // ???????????????
//            tableName = StringUtils.firstToLowerCase(tableName);
//        }
//        return tableName;
//    }
//
//    /**
//     * <p>
//     * ????????? ?????????,?????????
//     * </p>
//     *
//     * @param clazz        ?????????
//     * @param globalConfig ????????????
//     * @param tableInfo    ????????????????????????
//     */
//    public static void initTableFields(Class<?> clazz, GlobalConfig globalConfig, TableInfo tableInfo, List<String> excludeProperty) {
//        /* ????????????????????? */
//        GlobalConfig.DbConfig dbConfig = globalConfig.getDbConfig();
//        ReflectorFactory reflectorFactory = tableInfo.getConfiguration().getReflectorFactory();
//        //TODO @?????? ????????????????????????????????????.
//        Reflector reflector = reflectorFactory.findForClass(clazz);
//        List<Field> list = getAllFields(clazz);
//        // ???????????????????????????
//        boolean isReadPK = false;
//        /**
//         * ??????JPA ?????????
//         */
//        boolean jpaReadPK = false;
//        // ???????????? @TableId ??????
//        boolean existTableId = isExistTableId(list);
//
//        List<TableFieldInfo> fieldList = new ArrayList<>(list.size());
//        for (Field field : list) {
//            /**
//             * ???????????????????????? transient????????? ??????
//             */
//            final boolean exclude = excludeProperty.contains(field.getName());
//            final boolean transientField = Objects.nonNull(field.getAnnotation(Transient.class));
//            if (exclude || transientField) {
//                continue;
//            }
//
//            /* ??????ID ????????? */
//            if (existTableId) {
//                Id id = field.getAnnotation(Id.class);
//                TableId tableId = field.getAnnotation(TableId.class);
//                if (tableId != null || id != null) {
//                    if (isReadPK) {
//                        if (jpaReadPK) {
//                            throw ExceptionUtils.mpe("JPA @Id has been Init: \"%s\".", clazz.getName());
//                        } else {
//                            throw ExceptionUtils.mpe("@TableId can't more than one in Class: \"%s\".", clazz.getName());
//                        }
//                    } else {
//                        if (Objects.isNull(tableId)) {
//                            TableIdImp tableIdImp = new TableIdImp();
//                            Column column = field.getAnnotation(Column.class);
//                            if (Objects.nonNull(column)) {
//                                tableIdImp.setValue(column.name());
//                            }
//                            GeneratedValue generatedValue = field.getAnnotation(GeneratedValue.class);
//                            if (Objects.nonNull(generatedValue)) {
//                                tableIdImp.setType(IdType.AUTO);
//                                logger.warn("JPA compatible mode, Automatic growth is the only way to generate primary key");
//                            }
//                            tableId = tableIdImp;
//                            jpaReadPK = true;
//
//                        }
//                        initTableIdWithAnnotation(dbConfig, tableInfo, field, tableId, reflector);
//                        isReadPK = true;
//                        continue;
//                    }
//                }
//            } else if (!isReadPK) {
//                isReadPK = initTableIdWithoutAnnotation(dbConfig, tableInfo, field, reflector);
//                if (isReadPK) {
//                    continue;
//                }
//            }
//            TableField tableField = field.getAnnotation(TableField.class);
//
//            /* ??? @TableField ???????????????????????? */
//            if (tableField != null) {
//                fieldList.add(new TableFieldInfo(dbConfig, tableInfo, field, tableField, reflector));
//                continue;
//            } else {
//                final Column column = field.getAnnotation(Column.class);
//                if (Objects.nonNull(column)) {
//                    TableFieldImp tableFieldImp = new TableFieldImp();
//                    tableFieldImp.setValue(column.name());
//                    tableField = tableFieldImp;
//                    fieldList.add(new TableFieldInfo(dbConfig, tableInfo, field, tableField, reflector));
//                    continue;
//                }
//            }
//
//            /* ??? @TableField ???????????????????????? */
//            fieldList.add(new TableFieldInfo(dbConfig, tableInfo, field, reflector));
//        }
//
//        /* ????????????????????????????????????????????? */
//        Assert.isTrue(fieldList.parallelStream().filter(TableFieldInfo::isLogicDelete).count() < 2L,
//                String.format("@TableLogic can't more than one in Class: \"%s\".", clazz.getName()));
//
//        /* ????????????,??????????????? */
//        tableInfo.setFieldList(Collections.unmodifiableList(fieldList));
//
//        /* ?????????????????????????????????????????? */
//        if (!isReadPK) {
//            logger.warn(String.format("Can not find table primary key in Class: \"%s\".", clazz.getName()));
//        }
//    }
//
//    /**
//     * <p>
//     * ??????????????????????????????
//     * </p>
//     *
//     * @param list ????????????
//     * @return true ????????? @TableId ??????;
//     */
//    public static boolean isExistTableId(List<Field> list) {
//        return list.stream().anyMatch(field ->
//                field.isAnnotationPresent(TableId.class) ||
//                        field.isAnnotationPresent(Id.class));
//    }
//
//    /**
//     * <p>
//     * ?????????????????????
//     * </p>
//     *
//     * @param dbConfig  ??????????????????
//     * @param tableInfo ?????????
//     * @param field     ??????
//     * @param tableId   ??????
//     * @param reflector Reflector
//     */
//    private static void initTableIdWithAnnotation(GlobalConfig.DbConfig dbConfig, TableInfo tableInfo,
//                                                  Field field, TableId tableId, Reflector reflector) {
//        boolean underCamel = tableInfo.isUnderCamel();
//        final String property = field.getName();
//        if (field.getAnnotation(TableField.class) != null) {
//            logger.warn(String.format("This \"%s\" is the table primary key by @TableId annotation in Class: \"%s\",So @TableField annotation will not work!",
//                    property, tableInfo.getEntityType().getName()));
//        }
//        /* ??????????????? ?????? > ?????? ??? */
//        // ?????? Sequence ??????????????????
//        if (IdType.NONE == tableId.type()) {
//            tableInfo.setIdType(dbConfig.getIdType());
//        } else {
//            tableInfo.setIdType(tableId.type());
//        }
//
//        /* ?????? */
//        String column = property;
//        if (StringUtils.isNotBlank(tableId.value())) {
//            column = tableId.value();
//        } else {
//            // ???????????????????????????
//            if (underCamel) {
//                column = StringUtils.camelToUnderline(column);
//            }
//            // ??????????????????
//            if (dbConfig.isCapitalMode()) {
//                column = column.toUpperCase();
//            }
//        }
//        tableInfo.setKeyRelated(checkRelated(underCamel, property, column))
//                .setKeyColumn(column)
//                .setKeyProperty(property)
//                .setKeyType(reflector.getGetterType(property));
//    }
//
//    /**
//     * <p>
//     * ?????????????????????
//     * </p>
//     *
//     * @param tableInfo ?????????
//     * @param field     ??????
//     * @param reflector Reflector
//     * @return true ???????????????????????????????????? continue;
//     */
//    private static boolean initTableIdWithoutAnnotation(GlobalConfig.DbConfig dbConfig, TableInfo tableInfo,
//                                                        Field field, Reflector reflector) {
//        final String property = field.getName();
//        if (DEFAULT_ID_NAME.equalsIgnoreCase(property)) {
//            if (field.getAnnotation(TableField.class) != null) {
//                logger.warn(String.format("This \"%s\" is the table primary key by default name for `id` in Class: \"%s\",So @TableField will not work!",
//                        property, tableInfo.getEntityType().getName()));
//            }
//            String column = property;
//            if (dbConfig.isCapitalMode()) {
//                column = column.toUpperCase();
//            }
//            tableInfo.setKeyRelated(checkRelated(tableInfo.isUnderCamel(), property, column))
//                    .setIdType(dbConfig.getIdType())
//                    .setKeyColumn(column)
//                    .setKeyProperty(property)
//                    .setKeyType(reflector.getGetterType(property));
//            return true;
//        }
//        return false;
//    }
//
//    /**
//     * ?????? related ??????
//     * <p>
//     * ??? true ?????????????????????
//     *
//     * @param underCamel ????????????
//     * @param property   ?????????
//     * @param column     ?????????
//     * @return related
//     */
//    public static boolean checkRelated(boolean underCamel, String property, String column) {
//        column = StringUtils.getTargetColumn(column);
//        String propertyUpper = property.toUpperCase(Locale.ENGLISH);
//        String columnUpper = column.toUpperCase(Locale.ENGLISH);
//        if (underCamel) {
//            // ????????????????????? column ???????????????
//            return !(propertyUpper.equals(columnUpper) ||
//                    propertyUpper.equals(columnUpper.replace(StringPool.UNDERSCORE, StringPool.EMPTY)));
//        } else {
//            // ???????????????,???????????? property ????????? column ??????(?????????)
//            return !propertyUpper.equals(columnUpper);
//        }
//    }
//
//    /**
//     * <p>
//     * ?????????????????????????????????
//     * </p>
//     *
//     * @param clazz ?????????
//     * @return ????????????
//     */
//    public static List<Field> getAllFields(Class<?> clazz) {
//        List<Field> fieldList = ReflectionKit.getFieldList(ClassUtils.getUserClass(clazz));
//        return fieldList.stream()
//                .filter(field -> {
//                    /* ?????????????????????????????? */
//                    TableField tableField = field.getAnnotation(TableField.class);
//                    return (tableField == null || tableField.exist());
//                }).collect(toList());
//    }
//
//    public static KeyGenerator genKeyGenerator(String baseStatementId, TableInfo tableInfo, MapperBuilderAssistant builderAssistant) {
//        IKeyGenerator keyGenerator = GlobalConfigUtils.getKeyGenerator(builderAssistant.getConfiguration());
//        if (null == keyGenerator) {
//            throw new IllegalArgumentException("not configure IKeyGenerator implementation class.");
//        }
//        Configuration configuration = builderAssistant.getConfiguration();
//        //TODO ???????????????builderAssistant.getCurrentNamespace()????????????com.baomidou.mybatisplus.core.parser.SqlParserHelper.getSqlParserInfo???(chu)???(gui)
//        String id = builderAssistant.getCurrentNamespace() + StringPool.DOT + baseStatementId + SelectKeyGenerator.SELECT_KEY_SUFFIX;
//        ResultMap resultMap = new ResultMap.Builder(builderAssistant.getConfiguration(), id, tableInfo.getKeyType(), new ArrayList<>()).build();
//        MappedStatement mappedStatement = new MappedStatement.Builder(builderAssistant.getConfiguration(), id,
//                new StaticSqlSource(configuration, keyGenerator.executeSql(tableInfo.getKeySequence().value())), SqlCommandType.SELECT)
//                .keyProperty(tableInfo.getKeyProperty())
//                .resultMaps(Collections.singletonList(resultMap))
//                .build();
//        configuration.addMappedStatement(mappedStatement);
//        return new SelectKeyGenerator(mappedStatement, true);
//    }
//
//}

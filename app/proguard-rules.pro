# --- 通用抑制警告 ---
-dontwarn com.google.re2j.Matcher
-dontwarn com.google.re2j.Pattern

# --- 1. Room 数据库相关 ---
# 保留核心组件定义
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keep @androidx.room.Database class *

# 保留实体类成员（确保 Room 映射字段时不崩溃）
-keepclassmembers class top.sakimidare.seutimetable.data.model.** { *; }
-keep class top.sakimidare.seutimetable.data.model.** { *; }

# 保留类型转换器方法
-keepclassmembers class * {
    @androidx.room.TypeConverter <methods>;
}

# 保留 Room 自动生成的实现类及其构造函数
-keep class * extends androidx.room.RoomDatabase {
    <init>(...);
}
-keep class **.*_Impl { *; }

# --- 2. 数据解析与 JSON 相关 ---
# 保护 org.json 原生库
-keep class org.json.** { *; }

# 保护你的解析器逻辑（防止方法被内联或重命名导致反射失败）
-keep class top.sakimidare.seutimetable.data.parser.** { *; }

# 如果你未来使用 Kotlin Serialization，请务必保留序列化相关的注解和属性
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}

# --- 3. Glance 小组件与 DataStore 相关 ---
# 保护 Glance 核心组件和接收器
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver
-keep class * extends androidx.glance.appwidget.GlanceAppWidget
-keep class top.sakimidare.seutimetable.widgets.** { *; }

# 重点：保护 FORCE_REFRESH_KEY 所在的类成员（防止 Key 被混淆导致无法读取状态）
-keepclassmembers class top.sakimidare.seutimetable.widgets.*Kt {
    public static final androidx.datastore.preferences.core.Preferences$Key *;
}

# 保护 DataStore 内部机制（防止状态序列化失败）
-keep class androidx.datastore.** { *; }
-keepnames class androidx.datastore.preferences.core.** { *; }

# --- 4. 其它框架与协程支持 ---
# 保护 Kotlin 协程和 MainScope 相关逻辑（防止 Repository 作用域异常）
-keep class kotlinx.coroutines.** { *; }


# 防止JS被混淆
-keepclassmembers class top.sakimidare.seutimetable.ui.importing.WebAppInterface {
    @android.webkit.JavascriptInterface <methods>;
}
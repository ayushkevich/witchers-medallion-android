package by.alexy.witchersmedallion.domain

import android.content.Context
import androidx.annotation.StringRes

sealed class UiText {
    data class StringResource(
        @StringRes val id: Int,
        val args: List<Any> = emptyList(),
    ) : UiText()

    data class DynamicString(
        val value: String,
    ) : UiText()

    fun getString(context: Context): String = when (this) {
        is StringResource -> {
            context.getString(id, *args.toTypedArray())
        }
        is DynamicString -> value
    }

    companion object {
        fun fromStringResource(@StringRes id: Int, vararg args: Any): UiText = StringResource(id, args.toList())

        fun fromDynamicString(value: String): UiText = DynamicString(value)
    }
}

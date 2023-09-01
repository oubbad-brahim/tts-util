package org.jetbrains.anko

import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.Dialog
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.DialogInterface
import android.database.Cursor
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.storage.StorageManager
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewManager
import android.widget.ListAdapter
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class AnkoAsyncContext<T>(val weakRef: WeakReference<T>)

private val crashLogger = { throwable: Throwable -> throwable.printStackTrace() }

/**
 * Execute [task] asynchronously.
 *
 * @param exceptionHandler optional exception handler.
 *  If defined, any exceptions thrown inside [task] will be passed to it. If not, exceptions will be ignored.
 * @param task the code to execute asynchronously.
 */
fun <T> T.doAsync(
    exceptionHandler: ((Throwable) -> Unit)? = crashLogger,
    task: AnkoAsyncContext<T>.() -> Unit
): Future<Unit> {
    val context = AnkoAsyncContext(WeakReference(this))
    return BackgroundExecutor.submit {
        return@submit try {
            context.task()
        } catch (thr: Throwable) {
            val result = exceptionHandler?.invoke(thr)
            if (result != null) {
                result
            } else {
                Unit
            }
        }
    }
}

inline fun <reified T : View> View.find(@IdRes id: Int): T = findViewById(id)
inline fun <reified T : View> Activity.find(@IdRes id: Int): T = findViewById(id)

@Deprecated(message = "Use support library fragments instead. Framework fragments were deprecated in API 28.")
inline fun <reified T : View> Fragment.find(@IdRes id: Int): T = view?.findViewById(id) as T
inline fun <reified T : View> Dialog.find(@IdRes id: Int): T = findViewById(id)

internal object BackgroundExecutor {
    var executor: ExecutorService =
        Executors.newScheduledThreadPool(2 * Runtime.getRuntime().availableProcessors())

    fun <T> submit(task: () -> T): Future<T> = executor.submit(task)

}

/** Returns the NotificationManager instance. **/
val Context.notificationManager: NotificationManager
    get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

/** Returns the AudioManager instance. **/
val Context.audioManager: AudioManager
    get() = getSystemService(Context.AUDIO_SERVICE) as AudioManager

/**
 * Display the simple Toast message with the [Toast.LENGTH_SHORT] duration.
 *
 * @param message the message text resource.
 */
inline fun Context.toast(message: Int): Toast = Toast
    .makeText(this, message, Toast.LENGTH_SHORT)
    .apply {
        show()
    }

/**
 * Display the simple Toast message with the [Toast.LENGTH_SHORT] duration.
 *
 * @param message the message text.
 */
inline fun Context.toast(message: CharSequence): Toast = Toast
    .makeText(this, message, Toast.LENGTH_SHORT)
    .apply {
        show()
    }

/** Returns the StorageManager instance. **/
val Context.storageManager: StorageManager
    get() = getSystemService(Context.STORAGE_SERVICE) as StorageManager

/**
 * Display the simple Toast message with the [Toast.LENGTH_LONG] duration.
 *
 * @param message the message text.
 */
inline fun Context.longToast(message: CharSequence): Toast = Toast
    .makeText(this, message, Toast.LENGTH_LONG)
    .apply {
        show()
    }

/** Returns the AlarmManager instance. **/
val Context.alarmManager: AlarmManager
    get() = getSystemService(Context.ALARM_SERVICE) as AlarmManager

@Deprecated(message = "Inline", replaceWith = ReplaceWith("this"))
inline val Context.ctx: Context
    get() = this

/**
 * Execute [f] on the application UI thread.
 */
fun Context.runOnUiThread(f: Context.() -> Unit) {
    if (Looper.getMainLooper() === Looper.myLooper()) f() else ContextHelper.handler.post { f() }
}

private object ContextHelper {
    val handler = Handler(Looper.getMainLooper())
}

@Deprecated("Use AlertBuilder class instead.")
class AlertDialogBuilder(val ctx: Context) {
    private var builder: AlertDialog.Builder? = AlertDialog.Builder(ctx)

    /**
     * Returns the [AlertDialog] instance if created.
     * Returns null until the [show] function is called.
     */
    var dialog: AlertDialog? = null
        private set

    constructor(ankoContext: AnkoContext<*>) : this(ankoContext.ctx)

    fun dismiss() {
        dialog?.dismiss()
    }

    private fun checkBuilder() {
        if (builder == null) {
            throw IllegalStateException("show() was already called for this AlertDialogBuilder")
        }
    }

    /**
     * Create the [AlertDialog] and display it on screen.
     *
     */
    fun show(): AlertDialogBuilder {
        checkBuilder()
        dialog = builder!!.create()
        builder = null
        dialog!!.show()
        return this
    }

    /**
     * Set the [title] displayed in the dialog.
     */
    fun title(title: CharSequence) {
        checkBuilder()
        builder!!.setTitle(title)
    }

    /**
     * Set the title using the given [title] resource id.
     */
    fun title(title: Int) {
        checkBuilder()
        builder!!.setTitle(title)
    }

    /**
     * Set the [message] to display.
     */
    fun message(message: CharSequence) {
        checkBuilder()
        builder!!.setMessage(message)
    }

    /**
     * Set the message to display using the given [message] resource id.
     */
    fun message(message: Int) {
        checkBuilder()
        builder!!.setMessage(message)
    }

    /**
     * Set the resource id of the [Drawable] to be used in the title.
     */
    fun icon(icon: Int) {
        checkBuilder()
        builder!!.setIcon(icon)
    }

    /**
     * Set the [icon] Drawable to be used in the title.
     */
    fun icon(icon: Drawable) {
        checkBuilder()
        builder!!.setIcon(icon)
    }

    /**
     * Set the title using the custom [view].
     */
    fun customTitle(view: View) {
        checkBuilder()
        builder!!.setCustomTitle(view)
    }

    /**
     * Set the title using the custom DSL view.
     */
    fun customTitle(dsl: ViewManager.() -> Unit) {
        checkBuilder()
        val view = ctx.UI(dsl).view
        builder!!.setCustomTitle(view)
    }

    /**
     * Set a custom [view] to be the contents of the Dialog.
     */
    fun customView(view: View) {
        checkBuilder()
        builder!!.setView(view)
    }

    /**
     * Set a custom DSL view to be the contents of the Dialog.
     */
    fun customView(dsl: ViewManager.() -> Unit) {
        checkBuilder()
        val view = ctx.UI(dsl).view
        builder!!.setView(view)
    }

    /**
     * Set if the dialog is cancellable.
     *
     * @param cancellable if true, the created dialog will be cancellable.
     */
    fun cancellable(cancellable: Boolean = true) {
        checkBuilder()
        builder!!.setCancelable(cancellable)
    }

    /**
     * Sets the [callback] that will be called if the dialog is canceled.
     */
    fun onCancel(callback: () -> Unit) {
        checkBuilder()
        builder!!.setOnCancelListener { callback() }
    }

    /**
     * Sets the [callback] that will be called if a key is dispatched to the dialog.
     */
    fun onKey(callback: (keyCode: Int, e: KeyEvent) -> Boolean) {
        checkBuilder()
        builder!!.setOnKeyListener({ dialog, keyCode, event -> callback(keyCode, event) })
    }

    /**
     * Set a listener to be invoked when the neutral button of the dialog is pressed.
     *
     * @param neutralText the text resource to display in the neutral button.
     * @param callback the callback that will be called if the neutral button is pressed.
     */
    fun neutralButton(
        neutralText: Int = android.R.string.ok,
        callback: DialogInterface.() -> Unit = { dismiss() }
    ) {
        neutralButton(ctx.getString(neutralText), callback)
    }

    /**
     * Set a listener to be invoked when the neutral button of the dialog is pressed.
     *
     * @param neutralText the text to display in the neutral button.
     * @param callback the callback that will be called if the neutral button is pressed.
     */
    fun neutralButton(
        neutralText: CharSequence,
        callback: DialogInterface.() -> Unit = { dismiss() }
    ) {
        checkBuilder()
        builder!!.setNeutralButton(neutralText, { dialog, which -> dialog.callback() })
    }

    /**
     * Set a listener to be invoked when the positive button of the dialog is pressed.
     *
     * @param positiveText the text to display in the positive button.
     * @param callback the callback that will be called if the positive button is pressed.
     */
    fun positiveButton(positiveText: Int, callback: DialogInterface.() -> Unit) {
        positiveButton(ctx.getString(positiveText), callback)
    }

    /**
     * Set a listener to be invoked when the positive button of the dialog is pressed.
     *
     * @param callback the callback that will be called if the positive button is pressed.
     */
    fun okButton(callback: DialogInterface.() -> Unit) {
        positiveButton(ctx.getString(android.R.string.ok), callback)
    }

    /**
     * Set a listener to be invoked when the positive button of the dialog is pressed.
     *
     * @param callback the callback that will be called if the positive button is pressed.
     */
    fun yesButton(callback: DialogInterface.() -> Unit) {
        positiveButton(ctx.getString(android.R.string.yes), callback)
    }

    /**
     * Set a listener to be invoked when the positive button of the dialog is pressed.
     *
     * @param positiveText the text to display in the positive button.
     * @param callback the callback that will be called if the positive button is pressed.
     */
    fun positiveButton(positiveText: CharSequence, callback: DialogInterface.() -> Unit) {
        checkBuilder()
        builder!!.setPositiveButton(positiveText, { dialog, which -> dialog.callback() })
    }

    /**
     * Set a listener to be invoked when the negative button of the dialog is pressed.
     *
     * @param negativeText the text to display in the negative button.
     * @param callback the callback that will be called if the negative button is pressed.
     */
    fun negativeButton(negativeText: Int, callback: DialogInterface.() -> Unit = { dismiss() }) {
        negativeButton(ctx.getString(negativeText), callback)
    }

    /**
     * Set a listener to be invoked when the negative button of the dialog is pressed.
     *
     * @param callback the callback that will be called if the negative button is pressed.
     */
    fun cancelButton(callback: DialogInterface.() -> Unit = { dismiss() }) {
        negativeButton(ctx.getString(android.R.string.cancel), callback)
    }

    /**
     * Set a listener to be invoked when the negative button of the dialog is pressed.
     *
     * @param callback the callback that will be called if the negative button is pressed.
     */
    fun noButton(callback: DialogInterface.() -> Unit = { dismiss() }) {
        negativeButton(ctx.getString(android.R.string.no), callback)
    }

    /**
     * Set a listener to be invoked when the negative button of the dialog is pressed.
     *
     * @param negativeText the text to display in the negative button.
     * @param callback the callback that will be called if the negative button is pressed.
     */
    fun negativeButton(
        negativeText: CharSequence,
        callback: DialogInterface.() -> Unit = { dismiss() }
    ) {
        checkBuilder()
        builder!!.setNegativeButton(negativeText, { dialog, which -> dialog.callback() })
    }

    fun items(itemsId: Int, callback: (which: Int) -> Unit) {
        items(ctx.resources!!.getTextArray(itemsId), callback)
    }

    fun items(items: List<CharSequence>, callback: (which: Int) -> Unit) {
        items(items.toTypedArray(), callback)
    }

    fun items(items: Array<CharSequence>, callback: (which: Int) -> Unit) {
        checkBuilder()
        builder!!.setItems(items, { dialog, which -> callback(which) })
    }

    fun adapter(adapter: ListAdapter, callback: (which: Int) -> Unit) {
        checkBuilder()
        builder!!.setAdapter(adapter, { dialog, which -> callback(which) })
    }

    fun adapter(cursor: Cursor, labelColumn: String, callback: (which: Int) -> Unit) {
        checkBuilder()
        builder!!.setCursor(cursor, { dialog, which -> callback(which) }, labelColumn)
    }

}

inline fun Context.UI(
    setContentView: Boolean,
    init: AnkoContext<Context>.() -> Unit
): AnkoContext<Context> =
    createAnkoContext(this, init, setContentView)

inline fun <T> T.createAnkoContext(
    ctx: Context,
    init: AnkoContext<T>.() -> Unit,
    setContentView: Boolean = false
): AnkoContext<T> {
    val dsl = AnkoContextImpl(ctx, this, setContentView)
    dsl.init()
    return dsl
}

@DslMarker
private annotation class AnkoContextDslMarker

@AnkoContextDslMarker
interface AnkoContext<out T> : ViewManager {
    val ctx: Context
    val owner: T
    val view: View

    override fun updateViewLayout(view: View, params: ViewGroup.LayoutParams) {
        throw UnsupportedOperationException()
    }

    override fun removeView(view: View) {
        throw UnsupportedOperationException()
    }

    companion object {
        fun create(ctx: Context, setContentView: Boolean = false): AnkoContext<Context> =
            AnkoContextImpl(ctx, ctx, setContentView)

        fun createReusable(ctx: Context, setContentView: Boolean = false): AnkoContext<Context> =
            ReusableAnkoContext(ctx, ctx, setContentView)

        fun <T> create(ctx: Context, owner: T, setContentView: Boolean = false): AnkoContext<T> =
            AnkoContextImpl(ctx, owner, setContentView)

        fun <T> createReusable(
            ctx: Context,
            owner: T,
            setContentView: Boolean = false
        ): AnkoContext<T> = ReusableAnkoContext(ctx, owner, setContentView)

        fun <T : ViewGroup> createDelegate(owner: T): AnkoContext<T> = DelegatingAnkoContext(owner)
    }
}


internal class DelegatingAnkoContext<T : ViewGroup>(override val owner: T) : AnkoContext<T> {
    override val ctx: Context = owner.context
    override val view: View = owner

    override fun addView(view: View?, params: ViewGroup.LayoutParams?) {
        if (view == null) return

        if (params == null) {
            owner.addView(view)
        } else {
            owner.addView(view, params)
        }
    }
}

internal class ReusableAnkoContext<T>(
    override val ctx: Context,
    override val owner: T,
    setContentView: Boolean
) : AnkoContextImpl<T>(ctx, owner, setContentView) {
    override fun alreadyHasView() {}
}

open class AnkoContextImpl<T>(
    override val ctx: Context,
    override val owner: T,
    private val setContentView: Boolean
) : AnkoContext<T> {
    private var myView: View? = null

    override val view: View
        get() = myView ?: throw IllegalStateException("View was not set previously")

    override fun addView(view: View?, params: ViewGroup.LayoutParams?) {
        if (view == null) return

        if (myView != null) {
            alreadyHasView()
        }

        this.myView = view

        if (setContentView) {
            doAddView(ctx, view)
        }
    }

    private fun doAddView(context: Context, view: View) {
        when (context) {
            is Activity -> context.setContentView(view)
            is ContextWrapper -> doAddView(context.baseContext, view)
            else -> throw IllegalStateException("Context is not an Activity, can't set content view")
        }
    }

    open protected fun alreadyHasView(): Unit =
        throw IllegalStateException("View is already set: $myView")
}

inline fun Context.UI(init: AnkoContext<Context>.() -> Unit): AnkoContext<Context> =
    createAnkoContext(this, init)

@Deprecated(message = "Use support library fragments instead. Framework fragments were deprecated in API 28.")
inline fun Fragment.UI(init: AnkoContext<Fragment>.() -> Unit): AnkoContext<Fragment> =
    createAnkoContext(requireActivity(), init)

interface AnkoComponent<in T> {
    fun createView(ui: AnkoContext<T>): View
}

fun <T : Activity> AnkoComponent<T>.setContentView(activity: T): View =
    createView(AnkoContextImpl(activity, activity, true))
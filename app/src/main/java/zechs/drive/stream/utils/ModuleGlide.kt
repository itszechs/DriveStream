package zechs.drive.stream.utils

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import okhttp3.OkHttpClient
import java.io.InputStream
import java.util.concurrent.TimeUnit


@GlideModule
class ModuleGlide : AppGlideModule() {

    override fun registerComponents(
        context: Context, glide: Glide, registry: Registry
    ) {
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .build()

        glide.registry.replace(
            /* Class<Model> */ GlideUrl::class.java,
            /* Class<Data> */ InputStream::class.java,
            /* ModelLoaderFactory */ OkHttpUrlLoader.Factory(client)
        )
    }
}
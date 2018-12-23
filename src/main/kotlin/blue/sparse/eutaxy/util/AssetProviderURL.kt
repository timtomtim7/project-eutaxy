package blue.sparse.eutaxy.util

import blue.sparse.engine.asset.Asset
import blue.sparse.engine.asset.provider.AssetProvider
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object AssetProviderURL: AssetProvider {
	override fun get(path: String): Asset? {
		return try {
			URLAsset(URL(path)).takeIf(URLAsset::exists)
		}catch(t: Throwable) {
			null
		}
	}

	class URLAsset internal constructor(val url: URL): Asset {
		override val exists: Boolean
			get() {
				try {
					inputStream.close()
				}catch(t: Throwable) {
					return false
				}
				return true
			}

		override val inputStream: InputStream
			get() {
				val conn = url.openConnection() as HttpURLConnection
				conn.requestMethod = "GET"
				conn.doInput = true
				conn.setRequestProperty("User-Agent", "Eutaxy/1.0")

				return conn.inputStream
			}

		override val path: String
			get() = url.path

	}

}
package com.joseamc91.makertally

import android.app.Application
import com.joseamc91.makertally.data.androidRepository

class MakerTallyApplication : Application() {
    val repository by lazy { androidRepository(this) }
}

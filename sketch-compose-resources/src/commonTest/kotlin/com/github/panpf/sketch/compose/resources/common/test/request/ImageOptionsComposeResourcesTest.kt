package com.github.panpf.sketch.compose.resources.common.test.request

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.github.panpf.sketch.painter.equitablePainterResource
import com.github.panpf.sketch.painter.rememberEquitablePainterResource
import com.github.panpf.sketch.request.ComposableImageOptions
import com.github.panpf.sketch.request.error
import com.github.panpf.sketch.request.fallback
import com.github.panpf.sketch.request.placeholder
import com.github.panpf.sketch.state.PainterStateImage
import com.github.panpf.sketch.test.compose.resources.Res
import com.github.panpf.sketch.test.compose.resources.moon
import com.github.panpf.sketch.test.utils.Platform
import com.github.panpf.sketch.test.utils.current
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ImageOptionsComposeResourcesTest {

    @Test
    fun testPlaceholder() {
        if (Platform.current == Platform.iOS) {
            // Files in kotlin resources cannot be accessed in ios test environment.
            return
        }
        runComposeUiTest {
            setContent {
                ComposableImageOptions {
                    placeholder(Res.drawable.moon)
                }.apply {
                    assertEquals(
                        expected = PainterStateImage(equitablePainterResource(Res.drawable.moon)),
                        actual = placeholder
                    )
                }
            }
        }
    }

    @Test
    fun testFallback() {
        if (Platform.current == Platform.iOS) {
            // Files in kotlin resources cannot be accessed in ios test environment.
            return
        }
        runComposeUiTest {
            setContent {
                ComposableImageOptions {
                    fallback(Res.drawable.moon)
                }.apply {
                    assertEquals(
                        expected = PainterStateImage(equitablePainterResource(Res.drawable.moon)),
                        actual = fallback
                    )
                }
            }
        }
    }

    @Test
    fun testError() {
        if (Platform.current == Platform.iOS) {
            // Files in kotlin resources cannot be accessed in ios test environment.
            return
        }
        runComposeUiTest {
            setContent {
                ComposableImageOptions {
                    error(Res.drawable.moon)
                }.apply {
                    assertEquals(
                        expected = PainterStateImage(rememberEquitablePainterResource(Res.drawable.moon)),
                        actual = error
                    )
                }
            }
        }
    }
}
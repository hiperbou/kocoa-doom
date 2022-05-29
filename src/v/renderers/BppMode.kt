/*
 * Copyright (C) 2017 Good Sign
 * Copyright (C) 2022 hiperbou
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package v.renderers

import doom.CVarManager
import doom.CommandVariable
import doom.DoomMain
import m.Settings
import mochadoom.Engine
import rr.SceneRenderer
import v.DoomGraphicSystem
import v.renderers.BppMode
import v.renderers.RendererFactory.WithWadLoader
import java.awt.Transparency
import java.util.function.Function

/**
 * This class helps to choose proper components for bit depth
 * selected in config or through use of command line arguments
 */
enum class BppMode(lightBits: Int, renderGen: RenderGen<ByteArray, *>, scenerGen: ScenerGen<ByteArray, *>, transparency: Int) {
    Indexed(5,
        object : RenderGen<ByteArray, ByteArray> {
            override fun apply(rf: WithWadLoader<ByteArray, ByteArray>): SoftwareGraphicsSystem<ByteArray, ByteArray> {
                return BufferedRenderer(
                    rf
                ) as SoftwareGraphicsSystem<ByteArray, ByteArray>
            }
        },
        object : ScenerGen<ByteArray, ByteArray> {
            override fun apply(DOOM: DoomMain<ByteArray, ByteArray>): SceneRenderer<ByteArray, ByteArray> {
                return SceneGen_8(DOOM)
            }
        }, Transparency.OPAQUE
    ),
    HiColor(5,
        object : RenderGen<ByteArray, ShortArray> {
            override fun apply(rf: WithWadLoader<ByteArray, ShortArray>): SoftwareGraphicsSystem<ByteArray, ShortArray> {
                return BufferedRenderer16(
                    rf
                ) as SoftwareGraphicsSystem<ByteArray, ShortArray>
            }
        },
        object : ScenerGen<ByteArray, ShortArray> {
            override fun apply(DOOM: DoomMain<ByteArray, ShortArray>): SceneRenderer<ByteArray, ShortArray> {
                return SceneGen_16(DOOM)
            }
        }, Transparency.OPAQUE
    ),
    TrueColor(8,
        object : RenderGen<ByteArray, IntArray> {
            override fun apply(rf: WithWadLoader<ByteArray, IntArray>): SoftwareGraphicsSystem<ByteArray, IntArray> {
                return BufferedRenderer32(
                    rf
                ) as SoftwareGraphicsSystem<ByteArray, IntArray>
            }
        },
        object : ScenerGen<ByteArray, IntArray> {
            override fun apply(DOOM: DoomMain<ByteArray, IntArray>): SceneRenderer<ByteArray, IntArray> {
                return SceneGen_32(DOOM)
            }
        }, Transparency.OPAQUE
    ),
    AlphaTrueColor(8,
        object : RenderGen<ByteArray, IntArray> {
            override fun apply(rf: WithWadLoader<ByteArray, IntArray>): SoftwareGraphicsSystem<ByteArray, IntArray> {
                return BufferedRenderer32(
                    rf
                ) as SoftwareGraphicsSystem<ByteArray, IntArray>
            }
        },
        object : ScenerGen<ByteArray, IntArray> {
            override fun apply(DOOM: DoomMain<ByteArray, IntArray>): SceneRenderer<ByteArray, IntArray> {
                return SceneGen_32(DOOM)
            }
        }, Transparency.TRANSLUCENT
    );

    val transparency: Int
    val lightBits: Int
    private val renderGen: RenderGen<*, *>
    private val scenerGen: ScenerGen<*, *>

    init {
        this.lightBits = lightBits
        this.renderGen = renderGen
        this.scenerGen = scenerGen
        this.transparency = transparency
    }

    fun <T, V> graphics(rf: WithWadLoader<T, V>): DoomGraphicSystem<T, V> {
        return (renderGen as RenderGen<T, V>).apply(rf)
    }

    fun <T, V> sceneRenderer(DOOM: DoomMain<T, V>): SceneRenderer<T, V> {
        return (scenerGen as ScenerGen<T, V>).apply(DOOM)
    }

    internal interface ScenerGen<T, V> : Function<DoomMain<T, V>, SceneRenderer<T, V>>
    internal interface RenderGen<T, V> : Function<WithWadLoader<T, V>, SoftwareGraphicsSystem<T, V>>

    companion object {
        fun chooseBppMode(CVM: CVarManager): BppMode {
            return if (CVM.bool(CommandVariable.TRUECOLOR)) {
                TrueColor
            } else if (CVM.bool(CommandVariable.HICOLOR)) {
                HiColor
            } else if (CVM.bool(CommandVariable.INDEXED)) {
                Indexed
            } else if (CVM.bool(CommandVariable.ALPHATRUECOLOR)) {
                AlphaTrueColor
            } else {
                Engine.getConfig().getValue(Settings.color_depth, BppMode::class.java)
            }
        }

        private fun <T, V> SceneGen_8(DOOM: DoomMain<T, V>): SceneRenderer<T, V> {
            return SceneRendererMode.getMode().indexedGen.apply(DOOM as DoomMain<ByteArray, ByteArray>) as SceneRenderer<T, V>
        }

        private fun <T, V> SceneGen_16(DOOM: DoomMain<T, V>): SceneRenderer<T, V> {
            return SceneRendererMode.getMode().hicolorGen.apply(DOOM as DoomMain<ByteArray, ShortArray>) as SceneRenderer<T, V>
        }

        private fun <T, V> SceneGen_32(DOOM: DoomMain<T, V>): SceneRenderer<T, V> {
            return SceneRendererMode.getMode().truecolorGen.apply(DOOM as DoomMain<ByteArray, IntArray>) as SceneRenderer<T, V>
        }
    }
}

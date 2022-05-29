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

import doom.CommandVariable
import doom.DoomMain
import m.Settings
import mochadoom.Engine
import rr.SceneRenderer
import rr.UnifiedRenderer
import rr.parallel.ParallelRenderer
import rr.parallel.ParallelRenderer2
import java.util.function.Function


/**
 * This class helps to choose between scene renderers
 */
enum class SceneRendererMode(
    val indexedGen: SG<ByteArray, ByteArray>,
    val hicolorGen: SG<ByteArray, ShortArray>,
    val truecolorGen: SG<ByteArray, IntArray>
) {
    Serial(object : SG<ByteArray, ByteArray> {
        override fun apply(t: DoomMain<ByteArray, *>): SceneRenderer<ByteArray, *> {
            return UnifiedRenderer.Indexed(t as DoomMain<ByteArray?, ByteArray?>) as SceneRenderer<ByteArray, *>
        }},
        object : SG<ByteArray, ShortArray> {
            override fun apply(t: DoomMain<ByteArray, *>): SceneRenderer<ByteArray, *> {
                return UnifiedRenderer.HiColor(t as DoomMain<ByteArray?, ShortArray?>) as  SceneRenderer<ByteArray, *>
            }
        },
        object : SG<ByteArray, IntArray> {
            override fun apply(t: DoomMain<ByteArray, *>): SceneRenderer<ByteArray, *> {
                return UnifiedRenderer.TrueColor(t as DoomMain<ByteArray?, IntArray?>) as SceneRenderer<ByteArray, *>
            }
        }),
    Parallel(
        object : SG<ByteArray, ByteArray> {
            override fun apply(t: DoomMain<ByteArray, *>): SceneRenderer<ByteArray, *> {
                return SceneRendererMode.Parallel_8(t as DoomMain<ByteArray?, ByteArray?>) as SceneRenderer<ByteArray, *>
            }
        },
        object : SG<ByteArray, ShortArray> {
            override fun apply(t: DoomMain<ByteArray, *>): SceneRenderer<ByteArray, *> {
                return SceneRendererMode.Parallel_16(t as DoomMain<ByteArray?, ShortArray?>) as SceneRenderer<ByteArray, *>
            }
        },
        object : SG<ByteArray, IntArray> {
            override fun apply(t: DoomMain<ByteArray, *>): SceneRenderer<ByteArray, *> {
                return SceneRendererMode.Parallel_32(t as DoomMain<ByteArray?, IntArray?>) as SceneRenderer<ByteArray, *>
            }
        }),
    Parallel2(
        object : SG<ByteArray, ByteArray> {
            override fun apply(t: DoomMain<ByteArray, *>): SceneRenderer<ByteArray, *> {
                return SceneRendererMode.Parallel2_8(t as DoomMain<ByteArray?, ByteArray?>) as SceneRenderer<ByteArray, *>
            }
        },
        object : SG<ByteArray, ShortArray> {
            override fun apply(t: DoomMain<ByteArray, *>): SceneRenderer<ByteArray, *> {
                return SceneRendererMode.Parallel2_16(t as DoomMain<ByteArray?, ShortArray?>) as SceneRenderer<ByteArray, *>
            }
        },
        object : SG<ByteArray, IntArray> {
            override fun apply(t: DoomMain<ByteArray, *>): SceneRenderer<ByteArray, *> {
                return SceneRendererMode.Parallel2_32(t as DoomMain<ByteArray?, IntArray?>) as SceneRenderer<ByteArray, *>
            }
        });

    /*Serial(
        SG { DOOM: DoomMain<ByteArray?, ByteArray?>? -> Indexed(DOOM) },
        SG { DOOM: DoomMain<ByteArray?, ShortArray?>? -> HiColor(DOOM) },
        SG { DOOM: DoomMain<ByteArray?, IntArray?>? -> TrueColor(DOOM) }),
    Parallel(
        SG { DOOM: DoomMain<ByteArray?, ByteArray?>? -> SceneRendererMode.Parallel_8(DOOM) },
        SG { DOOM: DoomMain<ByteArray?, ShortArray?>? -> SceneRendererMode.Parallel_16(DOOM) },
        SG { DOOM: DoomMain<ByteArray?, IntArray?>? -> SceneRendererMode.Parallel_32(DOOM) }),
    Parallel2(
        SG { DOOM: DoomMain<ByteArray?, ByteArray?>? -> SceneRendererMode.Parallel2_8(DOOM) },
        SG { DOOM: DoomMain<ByteArray?, ShortArray?>? -> SceneRendererMode.Parallel2_16(DOOM) },
        SG { DOOM: DoomMain<ByteArray?, IntArray?>? -> SceneRendererMode.Parallel2_32(DOOM) });*/

    interface SG<T, out V> : Function<DoomMain<T, *>, SceneRenderer<T, *>>
    companion object {
        private val cVarSerial: Boolean = Engine.getCVM().bool(CommandVariable.SERIALRENDERER)
        private val cVarParallel: Boolean = Engine.getCVM().present(CommandVariable.PARALLELRENDERER)
        private val cVarParallel2: Boolean = Engine.getCVM().present(CommandVariable.PARALLELRENDERER2)
        private val threads =
            if (SceneRendererMode.cVarSerial) null else if (SceneRendererMode.cVarParallel) SceneRendererMode.parseSwitchConfig(
                CommandVariable.PARALLELRENDERER
            ) else if (SceneRendererMode.cVarParallel2) SceneRendererMode.parseSwitchConfig(
                CommandVariable.PARALLELRENDERER2
            ) else intArrayOf(2, 2, 2)

        fun parseSwitchConfig(sw: CommandVariable): IntArray {
            // Try parsing walls, or default to 1
            val walls: Int = Engine.getCVM().get<Int>(sw, Int::class.java, 0).orElse(1)
            // Try parsing floors. If wall succeeded, but floors not, it will default to 1.
            val floors: Int = Engine.getCVM().get<Int>(sw, Int::class.java, 1).orElse(1)
            // In the worst case, we will use the defaults.
            val masked: Int = Engine.getCVM().get<Int>(sw, Int::class.java, 2).orElse(2)
            return intArrayOf(walls, floors, masked)
        }

        fun getMode(): SceneRendererMode {
            if (SceneRendererMode.cVarSerial) {
                /**
                 * Serial renderer in command line argument will override everything else
                 */
                return Serial
            } else if (SceneRendererMode.cVarParallel) {
                /**
                 * The second-top priority switch is parallelrenderer (not 2) command line argument
                 */
                return SceneRendererMode.Parallel
            } else if (SceneRendererMode.cVarParallel2) {
                /**
                 * If we have parallelrenderer2 on command line, it will still override config setting
                 */
                return Parallel2
            }
            /**
             * We dont have overrides on command line - get mode from default.cfg (or whatever)
             * Set default parallelism config in this case
             * TODO: make able to choose in config, but on ONE line along with scene_renderer_mode, should be tricky!
             */
            return Engine.getConfig()
                .getValue(Settings.scene_renderer_mode, SceneRendererMode::class.java)
        }

        private fun Parallel_8(DOOM: DoomMain<ByteArray?, ByteArray?>): SceneRenderer<ByteArray?, ByteArray?> {
            return ParallelRenderer.Indexed(
                DOOM,
                threads!![0], threads[1], threads[2]
            )
        }

        private fun Parallel_16(DOOM: DoomMain<ByteArray?, ShortArray?>): SceneRenderer<ByteArray?, ShortArray?> {
            return ParallelRenderer.HiColor(
                DOOM,
                threads!![0], threads[1], threads[2]
            )
        }

        private fun Parallel_32(DOOM: DoomMain<ByteArray?, IntArray?>): SceneRenderer<ByteArray?, IntArray?> {
            return ParallelRenderer.TrueColor(
                DOOM,
                threads!![0], threads[1], threads[2]
            )
        }

        private fun Parallel2_8(DOOM: DoomMain<ByteArray?, ByteArray?>): SceneRenderer<ByteArray?, ByteArray?> {
            return ParallelRenderer2.Indexed(
                DOOM,
                threads!![0], threads[1], threads[2]
            )
        }

        private fun Parallel2_16(DOOM: DoomMain<ByteArray?, ShortArray?>): SceneRenderer<ByteArray?, ShortArray?> {
            return ParallelRenderer2.HiColor(
                DOOM,
                threads!![0], threads[1], threads[2]
            )
        }

        private fun Parallel2_32(DOOM: DoomMain<ByteArray?, IntArray?>): SceneRenderer<ByteArray?, IntArray?> {
            return ParallelRenderer2.TrueColor(
                DOOM,
                threads!![0], threads[1], threads[2]
            )
        }
    }
}
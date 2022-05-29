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

import v.renderers.DoomScreen
import java.util.*


/**
 *
 * @author Good Sign
 */
enum class DoomScreen {
    FG, BG, WS, WE, SB;

    companion object {
        fun <V> mapScreensToBuffers(bufferType: Class<V>, bufferLen: Int): Map<DoomScreen, V> {
            return Arrays.stream(DoomScreen.values())
                .collect({ EnumMap(DoomScreen::class.java) },
                    { map: EnumMap<DoomScreen, V>, screen: DoomScreen? ->
                        map[screen] = java.lang.reflect.Array.newInstance(bufferType.componentType, bufferLen) as V
                    }) { obj: EnumMap<DoomScreen, V>, m: EnumMap<DoomScreen, V> ->
                    obj.putAll(
                        m!!
                    )
                }
        }
    }
}
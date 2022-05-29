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

import v.DoomGraphicSystem
import v.scale.VideoScale
import w.IWadLoader
import java.util.*

/**
 * Renderer choice that depends on selected (or provided through command line) BppMode
 * It also ensures you create it in right order and with right components.
 *
 * And see - no package interface shared to public
 * @author Good Sign
 */
object RendererFactory {
    fun <T, V> newBuilder(): RendererFactory.Clear<T, V> {
        return RendererFactory.Builder()
    }

    private class Builder<T, V> : RendererFactory.Clear<T, V>, WithVideoScale<T, V>, WithBppMode<T, V>,
        WithWadLoader<T, V> {
        private var wadLoader: IWadLoader? = null
        private var videoScale: VideoScale? = null
        private var bppMode: BppMode? = null
        override fun setVideoScale(videoScale: VideoScale): RendererFactory.WithVideoScale<T, V> {
            this.videoScale = Objects.requireNonNull(videoScale)
            return this
        }

        override fun setBppMode(bppMode: BppMode): WithBppMode<T, V> {
            this.bppMode = Objects.requireNonNull(bppMode)
            return this
        }

        override fun setWadLoader(wadLoader: IWadLoader): WithWadLoader<T, V> {
            this.wadLoader = Objects.requireNonNull(wadLoader)
            return this
        }

        override fun build(): DoomGraphicSystem<T, V> {
            return bppMode!!.graphics(this)
        }

        override fun getBppMode(): BppMode? {
            return bppMode
        }

        override fun getVideoScale(): VideoScale? {
            return videoScale
        }

        override fun getWadLoader(): IWadLoader? {
            return wadLoader
        }
    }

    interface Clear<T, V> {
        fun setVideoScale(videoScale: VideoScale): WithVideoScale<T, V>
    }

    interface WithVideoScale<T, V> {
        fun setBppMode(bppMode: BppMode): WithBppMode<T, V>
        fun getVideoScale(): VideoScale?
    }

    interface WithBppMode<T, V> {
        fun setWadLoader(wadLoader: IWadLoader): WithWadLoader<T, V>
        fun getVideoScale(): VideoScale?
        fun getBppMode(): BppMode?
    }

    interface WithWadLoader<T, V> {
        fun build(): DoomGraphicSystem<T, V>
        fun getVideoScale(): VideoScale?
        fun getBppMode(): BppMode?
        fun getWadLoader(): IWadLoader?
    }
}
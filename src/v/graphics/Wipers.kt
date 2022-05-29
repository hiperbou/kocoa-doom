/**
 * Copyright (C) 1993-1996 Id Software, Inc.
 * from f_wipe.c
 *
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
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package v.graphics

import f.Wiper
import m.IRandom
import utils.GenericCopy
import v.graphics.Wipers.WipeFunc.WF

/**
 * SCREEN WIPE PACKAGE
 */
class Wipers : ColorTransform, Melt {
    companion object {
        private val instance = Wipers()

        fun <V, E : Enum<E>> createWiper(rnd: IRandom, screens: Screens<V, E>, ws: E, we: E, ms: E): Wiper {
            return WiperImpl(rnd, screens, ws, we, ms)
        }
    }


    /**
     * They are repeated thrice for a reason - they are overloads with different arguments
     * - Good Sign 2017/04/06
     *
     * ASS-WIPING functions
     */
    enum class WipeFunc {
        doColorXFormB(WF<ByteArray> { instance.colorTransformB(it) }, ByteArray::class.java),
        doColorXFormS(WF<ShortArray> { instance.colorTransformS(it) }, ShortArray::class.java),
        doColorXFormI(WF<IntArray> {instance.colorTransformI(it) }, IntArray::class.java),

        initColorXForm(WF<Any> { instance.initTransform(it) }),
        doColorXForm(doColorXFormB, doColorXFormS, doColorXFormI),
        exitColorXForm(WF<Any> { _ -> false } ),

        initScaledMelt(instance::initMeltScaled),
        doScaledMelt(instance::doMeltScaled),

        initMelt(instance::initMelt),
        doMelt(instance::doMelt),
        exitMelt(instance::exitMelt);

        private val supportFor: Class<*>?
        /*private*/ val func: WF<*>

        constructor(func: WF<*>) {
            supportFor = null
            this.func = func
        }

        constructor(func: WF<*>, supportFor: Class<out Any>) {
            this.supportFor = supportFor
            this.func = func
        }
        /*constructor(func: WF<*>, supportFor: Class<*>) {
            this.supportFor = supportFor
            this.func = func
        }*/

        constructor(vararg wf: WipeFunc) {
            fun <V> wipeChoice(wf: Array<out WipeFunc>): WF<V> {
                return WF<V> { wiper: WiperImpl<V, *> ->
                    var i = 0
                    while (i < wf.size) {
                        if (wiper.bufferType == wf[i].supportFor) {
                            val supported = wf[i].func as WF<V>
                            return@WF supported.invoke(wiper)
                        }
                        ++i
                    }
                    throw UnsupportedOperationException("Do not have support for: " + wiper.bufferType)
                }
            }

            supportFor = null
            func = wipeChoice<Any>(wf)
        }

        fun interface WF<V> {
            operator fun invoke(wiper: WiperImpl<V, *>): Boolean
        }
    }

    class WiperImpl<V, E : Enum<E>>(
        val random: IRandom,
        screens: Screens<V, E>,
        wipeStartScreen: E,
        wipeEndScreen: E,
        mainScreen: E
    ) : Wiper {
        private val relocation = Relocation(0, 0, 1)
        val screens: Screens<V, E>
        val bufferType: Class<*>
        val wipeStartScr: V
        val wipeEndScr: V
        val wipeScr: V
        val screenWidth: Int
        val screenHeight: Int
        val dupx: Int
        val dupy: Int
        val scaled_16: Int
        val scaled_8: Int
        var y: IntArray? = null
        var ticks = 0

        /** when false, stop the wipe  */
        @Volatile
        var go = false

        init {
            wipeStartScr = screens.getScreen(wipeStartScreen)!!
            wipeEndScr = screens.getScreen(wipeEndScreen)!!
            wipeScr = screens.getScreen(mainScreen)!!
            bufferType = wipeScr.javaClass
            this.screens = screens
            screenWidth = screens.getScreenWidth()
            screenHeight = screens.getScreenHeight()
            dupx = screens.getScalingX()
            dupy = screens.getScalingY()
            scaled_16 = dupy shl 4
            scaled_8 = dupy shl 3
        }

        fun startToScreen(source: Int, destination: Int) {
            screens.screenCopy(wipeStartScr, wipeScr, relocation.retarget(source, destination))
        }

        fun endToScreen(source: Int, destination: Int) {
            screens.screenCopy(wipeEndScr, wipeScr, relocation.retarget(source, destination))
        }

        /**
         * Sets "from" screen and stores it in "screen 2"
         */
        override fun StartScreen(x: Int, y: Int, width: Int, height: Int): Boolean {
            GenericCopy.memcpy(wipeScr, 0, wipeStartScr, 0, java.lang.reflect.Array.getLength(wipeStartScr))
            return false
        }

        /**
         * Sets "to" screen and stores it to "screen 3"
         */
        override fun EndScreen(x: Int, y: Int, width: Int, height: Int): Boolean {
            // Set end screen to "screen 3" and copy visible screen to it.
            GenericCopy.memcpy(wipeScr, 0, wipeEndScr, 0, java.lang.reflect.Array.getLength(wipeEndScr))
            // Restore starting screen.
            GenericCopy.memcpy(wipeStartScr, 0, wipeScr, 0, java.lang.reflect.Array.getLength(wipeScr))
            return false
        }

/*
        @SuppressWarnings("unchecked")
		private boolean invokeCheckedFunc(WipeFunc f) {
        	return ((WF<V>) f.func).invoke(this);
        }

 */
        private fun invokeCheckedFunc(f: WipeFunc): Boolean {
            return (f.func as WF<V>).invoke(this)
        }

        override fun ScreenWipe(type: WipeType, x: Int, y: Int, width: Int, height: Int, ticks: Int): Boolean {
            val rc: Boolean

            //System.out.println("Ticks do "+ticks);
            this.ticks = ticks

            // initial stuff
            if (!go) {
                go = true
                //wipe_scr = new byte[width*height]; // DEBUG
                // HOW'S THAT FOR A FUNCTION POINTER, BIATCH?!
                invokeCheckedFunc(type.getInitFunc())
            }

            // do a piece of wipe-in
            rc = invokeCheckedFunc(type.getDoFunc())
            // V.DrawBlock(x, y, 0, width, height, wipe_scr); // DEBUG

            // final stuff
            if (rc) {
                go = false
                invokeCheckedFunc(type.getExitFunc())
            }
            return !go
        }
    }

    interface WipeType {
        fun getInitFunc(): WipeFunc
        fun getDoFunc(): WipeFunc
        fun getExitFunc(): WipeFunc
    }
}
/*
 * Copyright (C) 1993-1996 Id Software, Inc.
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
package p


import doom.player_t
import doom.thinker_t
import mochadoom.Loggers


/**
 * In vanilla doom there is union called actionf_t that can hold
 * one of the three types: actionf_p1, actionf_v and actionf_p2
 *
 * typedef union
 * {
 * actionf_p1	acp1;
 * actionf_v	acv;
 * actionf_p2	acp2;
 *
 * } actionf_t;
 *
 * For those unfamiliar with C, the union can have only one value
 * assigned with all the values combined solving the behavior of
 * logical and of all of them)
 *
 * actionf_p1, actionf_v and actionf_p2 are defined as these:
 *
 * typedef  void (*actionf_v)();
 * typedef  void (*actionf_p1)( void* );
 * typedef  void (*actionf_p2)( void*, void* );
 *
 * As you can see, they are pointers, so they all occupy the same space
 * in the union: the length of the memory pointer.
 *
 * Effectively, this means that you can write to any of the three fields
 * the pointer to the function correspoding to the field, and
 * it will completely overwrite any other function assigned in other
 * two fields. Even more: the other fields will have the same pointer,
 * just with wrong type.
 *
 * In Mocha Doom, this were addressed differently. A special helper enum
 * was created to hold possible names of the functions, and they were checked
 * by name, not by equality of the objects (object == object if point the same)
 * assigned to one of three fields. But, not understanding the true nature
 * of C's unions, in Mocha Doom all three fields were preserved and threated
 * like they can hold some different information at the same time.
 *
 * I present hereby the solution that will both simplify the definition
 * and usage of the action functions, and provide a way to achieve the
 * exact same behavior as would be in C: if you assign the function,
 * you will replace the old one (virtually, "all the three fields")
 * and you can call any function with 0 to 2 arguments.
 *
 * Also to store the functions in the same place where we declare them,
 * an Command pattern is implemented, requiring the function caller
 * to provide himself or any sufficient class that implements the Client
 * contract to provide the information needed for holding the state
 * of action functions.
 *
 * - Good Sign 2017/04/28
 *
 * Thinkers can either have one parameter of type (mobj_t),
 * Or otherwise be sector specials, flickering lights etc.
 * Those are atypical and need special handling.
 */


interface ActiveStateParam
data class PlayerSpriteConsumer(val p: player_t, val s: pspdef_t):ActiveStateParam
data class ThinkerConsumer(val t: thinker_t):ActiveStateParam
data class MobjConsumer(val m: mobj_t):ActiveStateParam

interface ActiveStatesBase
fun interface ActiveState<T:ActiveStateParam>:ActiveStatesBase {
    fun accept(action:ActionFunctions, args: T)
}


fun interface PlayerSpriteActiveStates : ActiveState<PlayerSpriteConsumer>
fun interface ThinkerActiveStates : ActiveState<ThinkerConsumer>
fun interface MobjActiveStates : ActiveState<MobjConsumer>

interface ThinkerStates {
    val ordinal:Int
    val activeState:ActiveStatesBase
}
enum class RemoveState(override val activeState:ActiveStatesBase):ThinkerStates {
    REMOVE(ThinkerActiveStates { action, args -> action.nop(args.t) }),
}

enum class ActiveStates(override val activeState:ActiveStatesBase):ThinkerStates {
    NOP(ThinkerActiveStates { action, args -> action.nop(args.t) }),
    A_Light0(PlayerSpriteActiveStates { action, args -> action.A_Light0(args.p, args.s) }),
    A_WeaponReady(PlayerSpriteActiveStates { action, args -> action.A_WeaponReady(args.p, args.s) }),
    A_Lower(PlayerSpriteActiveStates { action, args -> action.A_Lower(args.p, args.s) }),
    A_Raise(PlayerSpriteActiveStates { action, args -> action.A_Raise(args.p, args.s) }),
    A_Punch(PlayerSpriteActiveStates { action, args -> action.A_Punch(args.p, args.s) }),
    A_ReFire(PlayerSpriteActiveStates { action, args -> action.A_ReFire(args.p, args.s) }),
    A_FirePistol(PlayerSpriteActiveStates { action, args -> action.A_FirePistol(args.p, args.s) }),
    A_Light1(PlayerSpriteActiveStates { action, args -> action.A_Light1(args.p, args.s) }),
    A_FireShotgun(PlayerSpriteActiveStates { action, args -> action.A_FireShotgun(args.p, args.s) }),
    A_Light2(PlayerSpriteActiveStates { action, args -> action.A_Light2(args.p, args.s) }),
    A_FireShotgun2(PlayerSpriteActiveStates { action, args -> action.A_FireShotgun2(args.p, args.s) }),
    A_CheckReload(PlayerSpriteActiveStates { action, args -> action.A_CheckReload(args.p, args.s) }),
    A_OpenShotgun2(PlayerSpriteActiveStates { action, args -> action.A_OpenShotgun2(args.p, args.s) }),
    A_LoadShotgun2(PlayerSpriteActiveStates { action, args -> action.A_LoadShotgun2(args.p, args.s) }),
    A_CloseShotgun2(PlayerSpriteActiveStates { action, args -> action.A_CloseShotgun2(args.p, args.s) }),
    A_FireCGun(PlayerSpriteActiveStates { action, args -> action.A_FireCGun(args.p, args.s) }),
    A_GunFlash(PlayerSpriteActiveStates { action, args -> action.A_GunFlash(args.p, args.s) }),
    A_FireMissile(PlayerSpriteActiveStates { action, args -> action.A_FireMissile(args.p, args.s) }),
    A_Saw(PlayerSpriteActiveStates { action, args -> action.A_Saw(args.p, args.s) }),
    A_FirePlasma(PlayerSpriteActiveStates { action, args -> action.A_FirePlasma(args.p, args.s) }),
    A_BFGsound(PlayerSpriteActiveStates { action, args -> action.A_BFGsound(args.p, args.s) }),
    A_FireBFG(PlayerSpriteActiveStates { action, args -> action.A_FireBFG(args.p, args.s) }),
    A_BFGSpray(MobjActiveStates { action, args -> action.A_BFGSpray(args.m) }),
    A_Explode(MobjActiveStates { action, args -> action.A_Explode(args.m) }),
    A_Pain(MobjActiveStates { action, args -> action.A_Pain(args.m) }),
    A_PlayerScream(MobjActiveStates { action, args -> action.A_PlayerScream(args.m) }),
    A_Fall(MobjActiveStates { action, args -> action.A_Fall(args.m) }),
    A_XScream(MobjActiveStates { action, args -> action.A_XScream(args.m) }),
    A_Look(MobjActiveStates { action, args -> action.A_Look(args.m) }),
    A_Chase(MobjActiveStates { action, args -> action.A_Chase(args.m) }),
    A_FaceTarget(MobjActiveStates { action, args -> action.A_FaceTarget(args.m) }),
    A_PosAttack(MobjActiveStates { action, args -> action.A_PosAttack(args.m) }),
    A_Scream(MobjActiveStates { action, args -> action.A_Scream(args.m) }),
    A_SPosAttack(MobjActiveStates { action, args -> action.A_SPosAttack(args.m) }),
    A_VileChase(MobjActiveStates { action, args -> action.A_VileChase(args.m) }),
    A_VileStart(MobjActiveStates { action, args -> action.A_VileStart(args.m) }),
    A_VileTarget(MobjActiveStates { action, args -> action.A_VileTarget(args.m) }),
    A_VileAttack(MobjActiveStates { action, args -> action.A_VileAttack(args.m) }),
    A_StartFire(MobjActiveStates { action, args -> action.A_StartFire(args.m) }),
    A_Fire(MobjActiveStates { action, args -> action.A_Fire(args.m) }),
    A_FireCrackle(MobjActiveStates { action, args -> action.A_FireCrackle(args.m) }),
    A_Tracer(MobjActiveStates { action, args -> action.A_Tracer(args.m) }),
    A_SkelWhoosh(MobjActiveStates { action, args -> action.A_SkelWhoosh(args.m) }),
    A_SkelFist(MobjActiveStates { action, args -> action.A_SkelFist(args.m) }),
    A_SkelMissile(MobjActiveStates { action, args -> action.A_SkelMissile(args.m) }),
    A_FatRaise(MobjActiveStates { action, args -> action.A_FatRaise(args.m) }),
    A_FatAttack1(MobjActiveStates { action, args -> action.A_FatAttack1(args.m) }),
    A_FatAttack2(MobjActiveStates { action, args -> action.A_FatAttack2(args.m) }),
    A_FatAttack3(MobjActiveStates { action, args -> action.A_FatAttack3(args.m) }),
    A_BossDeath(MobjActiveStates { action, args -> action.A_BossDeath(args.m) }),
    A_CPosAttack(MobjActiveStates { action, args -> action.A_CPosAttack(args.m) }),
    A_CPosRefire(MobjActiveStates { action, args -> action.A_CPosRefire(args.m) }),
    A_TroopAttack(MobjActiveStates { action, args -> action.A_TroopAttack(args.m) }),
    A_SargAttack(MobjActiveStates { action, args -> action.A_SargAttack(args.m) }),
    A_HeadAttack(MobjActiveStates { action, args -> action.A_HeadAttack(args.m) }),
    A_BruisAttack(MobjActiveStates { action, args -> action.A_BruisAttack(args.m) }),
    A_SkullAttack(MobjActiveStates { action, args -> action.A_SkullAttack(args.m) }),
    A_Metal(MobjActiveStates { action, args -> action.A_Metal(args.m) }),
    A_SpidRefire(MobjActiveStates { action, args -> action.A_SpidRefire(args.m) }),
    A_BabyMetal(MobjActiveStates { action, args -> action.A_BabyMetal(args.m) }),
    A_BspiAttack(MobjActiveStates { action, args -> action.A_BspiAttack(args.m) }),
    A_Hoof(MobjActiveStates { action, args -> action.A_Hoof(args.m) }),
    A_CyberAttack(MobjActiveStates { action, args -> action.A_CyberAttack(args.m) }),
    A_PainAttack(MobjActiveStates { action, args -> action.A_PainAttack(args.m) }),
    A_PainDie(MobjActiveStates { action, args -> action.A_PainDie(args.m) }),
    A_KeenDie(MobjActiveStates { action, args -> action.A_KeenDie(args.m) }),
    A_BrainPain(MobjActiveStates { action, args -> action.A_BrainPain(args.m) }),
    A_BrainScream(MobjActiveStates { action, args -> action.A_BrainScream(args.m) }),
    A_BrainDie(MobjActiveStates { action, args -> action.A_BrainDie(args.m) }),
    A_BrainAwake(MobjActiveStates { action, args -> action.A_BrainAwake(args.m) }),
    A_BrainSpit(MobjActiveStates { action, args -> action.A_BrainSpit(args.m) }),
    A_SpawnSound(MobjActiveStates { action, args -> action.A_SpawnSound(args.m) }),
    A_SpawnFly(MobjActiveStates { action, args -> action.A_SpawnFly(args.m) }),
    A_BrainExplode(MobjActiveStates { action, args -> action.A_BrainExplode(args.m) }),
    P_MobjThinker(MobjActiveStates { action, args -> action.P_MobjThinker(args.m) }),
    T_FireFlicker(ThinkerActiveStates { action, args -> action.T_FireFlicker(args.t) }),
    T_LightFlash(ThinkerActiveStates { action, args -> action.T_LightFlash(args.t) }),
    T_StrobeFlash(ThinkerActiveStates { action, args -> action.T_StrobeFlash(args.t) }),
    T_Glow(ThinkerActiveStates { action, args -> action.T_Glow(args.t) }),
    T_MoveCeiling(ThinkerActiveStates { action, args -> action.T_MoveCeiling(args.t) }),
    T_MoveFloor(ThinkerActiveStates { action, args -> action.T_MoveFloor(args.t) }),
    T_VerticalDoor(ThinkerActiveStates { action, args -> action.T_VerticalDoor(args.t) }),
    T_PlatRaise(ThinkerActiveStates { action, args -> action.T_PlatRaise(args.t) }),
    T_SlidingDoor(ThinkerActiveStates { action, args -> action.T_SlidingDoor(args.t) });

    companion object {
        private val LOGGER = Loggers.getLogger(ActiveStates::class.java.name)
    }
}

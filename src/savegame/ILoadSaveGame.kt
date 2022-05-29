package savegame


import p.ThinkerList

interface ILoadSaveGame {
    fun setThinkerList(li: ThinkerList?)
    fun doSave(li: ThinkerList?)
    fun doLoad(li: ThinkerList?)
}
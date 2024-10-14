package eyeriss.utils

import chisel3._
import chisel3.experimental._
import chisel3.util._

class AXI extends Bundle {
    val araddr  = Input(UInt(32.W))
    val arvalid = Input(Bool())
    val arready = Output(Bool())
    val arid    = Input(UInt(4.W))
    val arlen   = Input(UInt(8.W))
    val arsize  = Input(UInt(3.W))
    val arburst = Input(UInt(2.W))

    val rdata   = Output(UInt(32.W))
    val rresp   = Output(UInt(2.W))
    val rvalid  = Output(Bool())
    val rready  = Input(Bool())
    val rlast   = Output(Bool())
    val rid     = Output(UInt(4.W))

    val awaddr  = Input(UInt(32.W))
    val awvalid = Input(Bool())
    val awready = Output(Bool())
    val awid    = Input(UInt(4.W))
    val awlen   = Input(UInt(8.W))
    val awsize  = Input(UInt(3.W))
    val awburst = Input(UInt(2.W))

    val wdata   = Input(UInt(32.W))
    val wstrb   = Input(UInt(4.W))
    val wvalid  = Input(Bool())
    val wready  = Output(Bool())
    val wlast   = Input(Bool())

    val bresp   = Output(UInt(2.W))
    val bvalid  = Output(Bool())
    val bready  = Input(Bool())
    val bid     = Output(UInt(4.W))
}

class SimpBus extends Bundle {
    val ADDR    = Input(UInt(32.W))
    val REN     = Input(Bool())
    val RDATA   = Output(UInt(32.W))
    val WEN     = Input(Bool())
    val WMASK   = Input(UInt(4.W))
    val WDATA   = Input(UInt(32.W))
    val DONE    = Output(Bool())
}


class AXICtrl extends Module {

    val Port       = IO(new AXI)
    val SimpBus    = IO(Flipped(new SimpBus))
    
    val ReadReq     = Port.arvalid & Port.arready
    // Read FSM
    val sRIDLE :: sREAD :: Nil = Enum(2)
    val ReadState = RegInit(sRIDLE)
    ReadState := MuxLookup(ReadState, sRIDLE)(Seq(
        sRIDLE  -> Mux(Port.arvalid, sREAD, sRIDLE),
        sREAD   -> Mux(Port.rvalid & Port.rlast & Port.rready, sRIDLE, sREAD),
    ))
    Port.arready := (ReadState === sRIDLE) | ((ReadState === sREAD) & Port.rlast)
    // ARSIZE -> b010 -> 4B
    // ARBURST -> INCR
    val ARADDR      = RegEnable(Port.araddr     , 0.U   , ReadReq)
    val ARLEN       = RegEnable(Port.arlen      , 0.U   , ReadReq)
    val ARSIZE      = RegEnable(Port.arsize     , 0.U   , ReadReq)
    val ARBURST     = RegEnable(Port.arburst    , 0.U   , ReadReq)

    val ARLENCnt    = RegInit(0.U(8.W))
    ARLENCnt        := MuxLookup(ReadState, 0.U)(Seq(
        sREAD   -> Mux(Port.rvalid & Port.rready, Mux(Port.rlast, 0.U, ARLENCnt + 1.U), ARLENCnt)
    ))
    
    val PortREN     = MuxLookup(ReadState, false.B)(Seq(
        sRIDLE  -> ReadReq,
        sREAD   -> (Port.rvalid & ~Port.rlast & Port.rready),
    ))
    SimpBus.REN     := PortREN
    val RADDR    = MuxLookup(ReadState, 0.U)(Seq(
        sRIDLE  -> Port.araddr,
        sREAD   -> (ARADDR + Mux((ARBURST === 0.U), 0.U, ((1.U << ARSIZE) * (ARLENCnt + 1.U)))),
    ))

    Port.rdata  := Mux(Port.rvalid, SimpBus.RDATA, "hDEADBEEF".U)
    Port.rid    := 0.U
    Port.rlast  := (ReadState === sREAD) & (ARLENCnt === ARLEN)
    // read is always successful
    Port.rresp  := "b00".U
    Port.rvalid := (ReadState === sREAD)

    val WriteReq    = Port.awvalid & Port.awready
    val sWIDLE :: sWRITE :: sRESP :: Nil = Enum(3) 
    val WriteState  = RegInit(sWIDLE)
    WriteState      := MuxLookup(WriteState, sWIDLE)(Seq(
        sWIDLE  -> Mux(WriteReq ,sWRITE ,sWIDLE),
        sWRITE  -> Mux(Port.wvalid & Port.wready & Port.wlast, sRESP, sWRITE),
        sRESP   -> Mux(Port.bvalid & Port.bready, sWIDLE, sRESP),
    ))
    Port.awready    := WriteState === sWIDLE
    Port.wready     := WriteState === sWRITE
    Port.bvalid     := WriteState === sRESP
    Port.bresp      := "b00".U
    Port.bid        := 0.U

    val AWADDR      = RegEnable(Port.awaddr     , 0.U   , WriteReq)
    val AWLEN       = RegEnable(Port.awlen      , 0.U   , WriteReq)
    val AWSIZE      = RegEnable(Port.awsize     , 0.U   , WriteReq)
    val AWBURST     = RegEnable(Port.awburst    , 0.U   , WriteReq)

    val AWLENCnt    = RegInit(0.U(8.W))
    AWLENCnt        := MuxLookup(WriteState, 0.U)(Seq(
        sWRITE  -> Mux(Port.wvalid & Port.wready & ~Port.wlast, AWLENCnt + 1.U, AWLENCnt),
        sRESP   -> Mux(Port.bvalid & Port.bready, 0.U, AWLENCnt),
    ))

    SimpBus.WEN     := Port.wvalid & Port.wready
    val WADDR       = AWADDR + Mux((AWBURST === 0.U), 0.U, (AWLENCnt * (1.U << AWSIZE)))
    SimpBus.WMASK   := Port.wstrb
    SimpBus.WDATA   := Port.wdata

    SimpBus.ADDR    := Mux(SimpBus.WEN, WADDR, RADDR)
} // AXICtrl


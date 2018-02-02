package tests

import chisel3._
import chisel3.util._
import chisel3.iotesters._
import org.scalatest.{Matchers, FlatSpec}

import Common._

class DebugPeekPokeTester(c: DebugModule) extends PeekPokeTester(c)  {

  val req = c.io.dmi.req
  val resp = c.io.dmi.resp
  val mreq = c.io.debugmem.req
  val mresp = c.io.debugmem.resp 

  /// INITIALIZE
  poke(req.bits.addr, 0)
  poke(req.bits.data, 0)
  poke(req.bits.op, 0)
  poke(req.valid, 0)
  poke(resp.ready, 1)
  poke(mreq.ready, 1)
  poke(mresp.valid, 0)
  poke(mresp.bits.data, 0)

    step(1)
  /// DMI Reg W/R Test 
  poke(req.bits.addr, DMI_RegAddrs.DMI_SBADDRESS0)
  poke(req.bits.data, 0x1000)
  poke(req.valid, 1)
  poke(req.bits.op, 2) // write
  expect(req.ready, 1)
    while(peek(resp.valid) == BigInt(0)) { step(1) }
  expect(resp.valid, 1)
  poke(req.valid, 0)
    step(1)
  poke(req.bits.addr, DMI_RegAddrs.DMI_SBADDRESS0)
  poke(req.valid, 1)
  poke(req.bits.op, 1) // read
  expect(req.ready, 1)
    step(1)
  expect(resp.bits.data, 0x1000)
  expect(resp.valid, 1)
  poke(req.valid, 0)

    step(1)
  /// Debug Mem Write Test
  poke(req.bits.addr, DMI_RegAddrs.DMI_SBDATA0)
  poke(req.valid, 1)
  poke(req.bits.op, 1) // read
  expect(req.ready, 1)
    step(1)
  poke(req.valid, 0)
  expect(mreq.valid, 1)
  expect(mreq.bits.addr, 0x1000)
  expect(mreq.bits.fcn, 0) // for read
  poke(mresp.valid, 1)
  poke(mresp.bits.data, 35)
    step(1)
  expect(resp.bits.data, 35)
  expect(resp.valid, 1)

    step(1)
  // DMI NON-STANDARD Reset
  poke(req.bits.addr, 0x44)
  poke(req.valid, 1)
  expect(c.io.resetcore, 1) // Default in reset
    step(1)
  expect(c.io.resetcore, 0)

}

class DebugTests extends FlatSpec with Matchers {
  behavior of "DebugModule"

  val manager = new TesterOptionsManager {
    testerOptions = testerOptions.copy(backendName = "firrtl") // firrtl or verilator
  }

  it should "check Debug Module " in {
    implicit val sodor_conf = SodorConfiguration()
    chisel3.iotesters.Driver.execute(() => new DebugModule,manager) ( c =>
      new DebugPeekPokeTester(c)
    ) should be(true)
  }
}

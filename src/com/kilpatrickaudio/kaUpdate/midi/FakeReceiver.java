package com.kilpatrickaudio.kaUpdate.midi;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;

public class FakeReceiver implements Receiver {

	public FakeReceiver() {
		System.out.println("FakeReceiver() - pretending to send MIDI");
	}
	
	@Override
	public void close() {
		System.out.println("FakeReceiver - close()");
	}

	@Override
	public void send(MidiMessage msg, long timestamp) {
		System.out.print("FakeReceiver - send() - time: " + timestamp + " - ");
		byte data[] = msg.getMessage();
		for(int i = 0; i < data.length; i ++) {
			System.out.print((data[i] & 0xff) + " ");
		}
		System.out.println();
	}

}

# Embedded Distance Display System

This project is an embedded systems application that integrates sensing, control, and
communication on an Arduino microcontroller.

The system periodically measures distance using an ultrasonic sensor and processes the
raw timing data on an Arduino microcontroller. The main control loop is implemented in a
non-blocking, event-driven manner using delta timing and multiple finite state machines
(FSMs), allowing sensor acquisition, user input, and serial communication to operate
concurrently. Measured distance values are visualized locally through a servo driven analog dial and a
PWM-controlled LED bar graph with adjustable brightness. In parallel, the Arduino
communicates bidirectionally with a Java application over a custom serial protocol,
enabling remote monitoring and dynamic control of display modes.

## What I Built
- A non-blocking Arduino system using delta timing (no `delay()` in the main loop)
- Multiple finite state machines (FSMs) for:
  - Button debouncing
  - Display mode control
  - Serial protocol parsing
- A custom binary serial protocol (magic number + key–value messages)
- A Java-based serial receiver that parses messages using an FSM
- Real-time visualization via:
  - Servo motor dial
  - LED bar graph with variable brightness (PWM)

## System Architecture
- **Arduino**
  - Ultrasonic distance sensing (periodic sampling)
  - Servo and LED display control
  - Bidirectional serial communication
- **Java**
  - Non-blocking serial input handling
  - Protocol message parsing
  - Distance conversion and command interface

## Display Modes
- Dial only
- LED bar graph (full intensity)
- LED bar graph (half intensity)
- Dial + LED bar graph

## Notes on Coursework
This project is based on coursework from a computer engineering class
and was independently refactored and extended into a standalone embedded systems project.
This project originated from coursework

## Technologies
- Arduino (C)
- Java
- Serial communication
- Finite state machines
- PWM motor and LED control

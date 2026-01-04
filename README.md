# Embedded Distance Display System

This project is based on coursework from an intro to CE class
and was independently refactored and extended into a standalone embedded systems project.

The system measures distance using an ultrasonic sensor and displays the result using
a servo-driven dial and a PWM-controlled LED bar graph. It also communicates with a
Java application over a custom serial protocol.

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
This project originated from coursework, but:
- All code in this repository has been reorganized and refactored
- Course-provided instructions, skeleton code, and assignment text are not included

## Technologies
- Arduino (C/C++)
- Java
- Serial communication
- Finite state machines
- PWM motor and LED control

#!/bin/bash

echo "Generating feature graphics to ~/alpine-icons/alpine-feature-graphic.png..."
mkdir -p ~/alpine-icons/
rsvg-convert feature-graphic.svg > ~/alpine-icons/feature-graphic.png

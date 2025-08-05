#!/usr/bin/env bash

kill "$(ps -A | grep -E "update-[0-9]+\.jar" | awk 'NR==1{print $1}')"
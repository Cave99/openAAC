SHELL := /bin/bash

APK_PATH := app/build/outputs/apk/debug/app-debug.apk
APP_ID := com.openaac.app
GRADLEW := ./gradlew
ADB := adb

LOCAL_JAVA_HOME := /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
LOCAL_ANDROID_HOME := /opt/homebrew/share/android-commandlinetools

ifeq ($(wildcard $(LOCAL_JAVA_HOME)), $(LOCAL_JAVA_HOME))
export JAVA_HOME := $(LOCAL_JAVA_HOME)
endif

ifeq ($(wildcard $(LOCAL_ANDROID_HOME)), $(LOCAL_ANDROID_HOME))
export ANDROID_HOME := $(LOCAL_ANDROID_HOME)
export PATH := $(JAVA_HOME)/bin:$(ANDROID_HOME)/platform-tools:$(PATH)
endif

.PHONY: APK apk check doctor lint test tablet install uninstall clean

APK:
	$(GRADLEW) assembleDebug
	@echo "Updated $(APK_PATH)"

apk: APK

check: doctor
	$(GRADLEW) check

lint: doctor
	$(GRADLEW) lint

test: doctor
	$(GRADLEW) testDebugUnitTest

doctor:
	@java_version="$$(java -version 2>&1 | awk -F '"' '/version/ { print $$2 }')"; \
	major="$$(printf '%s\n' "$$java_version" | awk -F. '{ if ($$1 == "1") print $$2; else print $$1 }')"; \
	if [[ -z "$$major" || "$$major" -lt 17 ]]; then \
		echo "Java 17+ is required. Current java version: $${java_version:-unknown}"; \
		echo "This Makefile will use JAVA_HOME=$(JAVA_HOME) when available."; \
		exit 1; \
	fi
	@[[ -n "$$ANDROID_HOME" ]] || { echo "ANDROID_HOME is not set"; exit 1; }
	@echo "Java: $$(java -version 2>&1 | sed -n '1p')"
	@echo "ANDROID_HOME=$$ANDROID_HOME"

tablet: APK
	@command -v $(ADB) >/dev/null 2>&1 || { echo "adb was not found on PATH"; exit 1; }
	@if [[ -n "$$ANDROID_SERIAL" ]]; then \
		echo "Installing $(APK_PATH) on $$ANDROID_SERIAL"; \
		$(ADB) -s "$$ANDROID_SERIAL" install -r "$(APK_PATH)"; \
		exit $$?; \
	fi; \
	devices="$$( $(ADB) devices | awk 'NR > 1 && $$2 == "device" { print $$1 }' )"; \
	count="$$( printf '%s\n' "$$devices" | sed '/^$$/d' | wc -l | tr -d ' ' )"; \
	if [[ "$$count" == "0" ]]; then \
		echo "No USB-connected Android tablet found. Connect the tablet, enable USB debugging, then run 'make tablet' again."; \
		exit 1; \
	elif [[ "$$count" != "1" ]]; then \
		echo "Multiple Android devices are connected; set ANDROID_SERIAL=<serial> and run 'make tablet' again."; \
		$(ADB) devices; \
		exit 1; \
	fi; \
	serial="$$( printf '%s\n' "$$devices" | sed -n '1p' )"; \
	echo "Installing $(APK_PATH) on $$serial"; \
	$(ADB) -s "$$serial" install -r "$(APK_PATH)"

install: tablet

uninstall:
	$(ADB) uninstall $(APP_ID)

clean:
	$(GRADLEW) clean

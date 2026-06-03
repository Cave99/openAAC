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

.PHONY: APK apk tablet install uninstall clean

APK:
	$(GRADLEW) assembleDebug
	@echo "Updated $(APK_PATH)"

apk: APK

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

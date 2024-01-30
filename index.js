import { NativeModules, Platform } from "react-native";

const { RNBrptouchprinter } = NativeModules;

const component = NativeModules.RNBrptouchprinter;

class BRPtouchPrinter {
	async print(macAddress, pdfPath, labelType) {
		return await component.print(macAddress, pdfPath, labelType);
	}

	async getConnectedPrinters() {
		return await component.getConnectedPrinters();
	}
}

export default BRPtouchPrinter;

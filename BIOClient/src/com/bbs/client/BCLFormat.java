/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bbs.client;

/**
 *
 * @author Arkangel
 */
public enum BCLFormat {

        UNKNOW(""),
        BCL_ANSI_378("ansi-fmr"),
        BCL_AAMVA("aamva-fmr"),
        BCL_ISO_19794_2("iso-fmr"),
        BCL_ISO_19794_2_NS("iso-fmc-ns"),
        BCL_ISO_19794_2_CS("iso-fmc-cs"),
        BCL_DIN_V66400_CS("din-cs"),
        BCL_ILO_FMR("ilo-fmr"),
        BCL_MINEX_A("minex-a"),
        BCL_PKMAT("pkmat"),
        BCL_PKM("PKM", "pkmat", "pkm"),
        BCL_PKMOC("pkmoc"),
        BCL_PKCOMP("pkcomp"),
        BCL_PKCOMPV1("pkcomp-v1"),
        BCL_PKCOMPV2("pkcomp-v2"),
        BCL_CFV("cfv"),
        BCL_TEXT("txt"),
        BCL_PKMAT64BITS("PKMat64Bits", "pkmat", "txt");
        private String format;
        private String extension;
        private String name = "";

        BCLFormat(String format) {
            this.format = format;
            this.extension = format;
        }

        BCLFormat(String format, String extension) {
            this.format = format;
            this.extension = extension;
        }

        BCLFormat(String name, String format, String extension) {
            this.format = format;
            this.extension = extension;
            this.name = name;
        }

        @Override
        public String toString() {
            return name.isEmpty() ? format : name;
        }

        public String getExtension() {
            return extension;
        }

        public String getFormat() {
            return format;
        }
    }
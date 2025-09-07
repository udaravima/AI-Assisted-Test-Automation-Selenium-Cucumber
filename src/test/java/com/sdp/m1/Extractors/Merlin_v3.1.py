import os
import re
import json
import camelot
import pdfplumber
import pandas as pd

SRC_ROOT = "Documents/"
SRC_FILE = f"{SRC_ROOT}M1-SDP-PRO-SRS_v0.1.0_annotated.pdf"
OUTPUT_DIR = f"{SRC_ROOT}SRS_Fields_v3.91"

FIELD_TABLE_HEADERS = ["Field", "Description",
                       "Type", "Validation", "Error Response"]


def clean_text(val):
    """Remove non-displayable characters and normalize whitespace."""
    if not isinstance(val, str):
        return val
    replacements = {
        '\u00ad': '-', '\u2013': '-', '\u2014': '-',
        '\u2018': "'", '\u2019': "'", '\u2026': '...',
        '\u201c': '"', '\u201d': '"', '\u2022': '*',
        '\u2025': '..', '\u00a0': ' ', '\u00c9': 'E',
        '\xad': '-'
    }
    for char, replacement in replacements.items():
        val = val.replace(char, replacement)
    val = re.sub(r"\n", " ", val)  # replace newlines with spaces
    val = re.sub(r"[^\x20-\x7E\t]", " ", val)  # strip non-printables
    val = re.sub(r"\s+", " ", val)
    return val.strip()


def extract_sections():
    """Extract text by page using pdfplumber to detect section headings & requirements."""
    sections = {}
    with pdfplumber.open(SRC_FILE) as pdf:
        for i, page in enumerate(pdf.pages, start=1):
            text = page.extract_text() or ""
            sections[i] = clean_text(text)
    return sections


def detect_context(page_text):
    """Find nearest section, requirement ID, and figure reference from text."""
    section_match = re.findall(
        r"(\d+(:?\.\d+)*\s+(:?[A-Z][a-z]+\s))+", page_text)
    req_match = re.findall(r"(REQ-[A-Z0-9\- ]+:[ A-Za-z0-9]+)", page_text)
    fig_match = re.findall(r"Figure\s+\d+[-–]\d+:[A-Za-z0-9 ]+", page_text)
    # Debugging
    # print("Sections:", section_match)
    # print("Requirements:", req_match)
    # print("Figures:", fig_match)

    # section = section_match[-1] if section_match else None
    # requirement = req_match[-1] if req_match else None
    # figure = fig_match[-1] if fig_match else None

    return section_match, req_match, fig_match


def normalize_headers(df):
    """Clean headers and cells in dataframe."""
    df = df.dropna(axis=1, how="all").dropna(axis=0, how="all")
    df = df.reset_index(drop=True)
    df = df.map(clean_text)

    header_candidates = FIELD_TABLE_HEADERS
    if any(h in df.iloc[0].tolist() for h in header_candidates):
        df.columns = df.iloc[0]
        df = df.drop(0).reset_index(drop=True)
    else:
        df.columns = [f"Column_{i}" for i in range(len(df.columns))]

    df.columns = [clean_text(str(c)).strip() for c in df.columns]
    return df


def isNullTable(table):
    if (table == '').all().all():
        return True
    return False


def merge_tables(tables):
    """Merge by column count across pages, then return merged tables with page info."""
    merged = []
    if not tables:
        return merged

    # current_df = normalize_headers(tables[0].df)
    current_df = tables[0].df
    current_page = tables[0].page
    pages = [current_page]

    for i in range(1, len(tables)):
        # next_df = normalize_headers(tables[i].df)
        if isNullTable(tables[i].df):
            continue
        next_df = tables[i].df
        next_page = tables[i].page

        if current_df.shape[1] == next_df.shape[1] and abs(current_page - next_page) <= 1:
            # merge and drop if first column of first row is empty
            if (next_df.iloc[0, 0] == ''):
                current_df.iloc[-1, :] += next_df.iloc[0, :]
                next_df = next_df.iloc[1:].reset_index(drop=True)

            current_df = pd.concat([current_df, next_df], ignore_index=True)
            current_page = next_page
            pages.append(current_page)
        else:
            merged.append((normalize_headers(current_df), pages))
            current_df = next_df
            current_page = next_page
            pages = [current_page]

    merged.append((normalize_headers(current_df), pages))
    return merged


def classify_table(df):
    """Classify as 'field-table' or 'other'."""
    headers = [str(h).strip().lower() for h in df.columns]
    field_headers = [h.lower() for h in FIELD_TABLE_HEADERS]

    if any(h in headers for h in field_headers):
        return "field-table"
    return "other-table"


def postprocess_field_table(df):
    """Fix field tables: fill down first column for expanded rows."""
    if "Field" in df.columns:
        df["Field"] = df["Field"].replace("", pd.NA).fillna(method="ffill")
    return df


def save_table(df, metadata, base_name):
    """Save JSON + CSV with metadata included in JSON."""
    if not os.path.exists(OUTPUT_DIR):
        os.makedirs(OUTPUT_DIR)
        os.makedirs(f"{OUTPUT_DIR}/json")
        os.makedirs(f"{OUTPUT_DIR}/csv")

    # Clean cells
    df = df.map(clean_text)

    # JSON with metadata
    records = df.to_dict(orient="records")
    json_path = f"{OUTPUT_DIR}/json/{base_name}.json"
    with open(json_path, "w", encoding="utf-8") as f:
        json.dump({
            "metadata": metadata,
            "table": records
        }, f, indent=2, ensure_ascii=False)

    # CSV
    df.to_csv(f"{OUTPUT_DIR}/csv/{base_name}.csv", index=False)


def main():
    print("🔎 Extracting sections from PDF...")
    sections = extract_sections()

    print("📑 Extracting tables with Camelot...")
    tables = camelot.read_pdf(SRC_FILE, pages="1-end", flavor="lattice",
                              split_text=True, flag_size=True, parallel=True)

    print(f"➡ Found {len(tables)} raw table fragments.")

    merged_tables = merge_tables(tables)
    print(f"✅ Merged into {len(merged_tables)} final tables.")

    for idx, (df, pages) in enumerate(merged_tables):
        page_text = "".join(sections.get(idx, "") for idx in pages)
        section, requirement, figure = detect_context(page_text)
        table_type = classify_table(df)
        # if table_type == "field-table":
        #     df = postprocess_field_table(df)
        l_sec = list(*section[-1]) if section else None
        # debugging
        print(f"Entire Section: {section}")
        print(f"Last section: {l_sec}")
        sec_l = l_sec.replace(" ", "_").replace(
            ".", "-") if l_sec else ""
        safe_section = f"page_{pages[-1]}"
        base_name = f"{safe_section}_{table_type}_table{idx}_{sec_l}"

        metadata = {
            "pages": pages,
            "section": [*section],
            "requirement": [*requirement],
            "figure": [*figure],
            "headers": list(df.columns),
            "type": table_type
        }

        save_table(df, metadata, base_name)
        print(f"📌 Saved {base_name} (rows={len(df)})")

    print(
        f"\n🎉 Done! Extracted and merged {len(merged_tables)} tables into {OUTPUT_DIR}/json and /csv.")


if __name__ == "__main__":
    main()

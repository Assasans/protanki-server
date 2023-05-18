use std::{
  env,
  path::Path,
  fs::File,
  io::{BufReader, Write},
  collections::BTreeMap,
  fmt::{self, Write as FmtWrite}
};
use serde::{Deserialize, Serialize};
use serde_with::serde_as;
use indoc::formatdoc;

#[serde_as]
#[derive(Serialize, Deserialize, Clone, Debug)]
struct PacketDefinition {
  name: Option<String>,
  model: i32,
  #[serde_as(as = "BTreeMap<_, _>")]
  fields: Vec<(String, String)>
}

fn generate_struct(name: &str, fields: &Vec<(String, String)>) -> String {
  formatdoc!(
    r#"
    #[derive(Default, Clone, Debug)]
    pub struct {name} {{
    {fields}
    }}
    "#,
    name = name,
    fields = fields.iter()
      .map(|(name, codec)| format!("  pub {name}: {codec},"))
      .collect::<Vec<_>>()
      .join("\n")
  )
}

fn generate_codec(name: &str, fields: &Vec<(String, String)>) -> String {
  formatdoc!(
    r#"
    #[allow(unused_variables)]
    #[allow(unused_mut)]
    impl Codec for {name} {{
      type Target = Self;

      fn encode(&self, registry: &CodecRegistry, writer: &mut dyn Write, value: &Self::Target) -> CodecResult<()> {{
    {encode}
        Ok(())
      }}

      fn decode(&self, registry: &CodecRegistry, reader: &mut dyn Read) -> CodecResult<Self::Target> {{
        let mut value = Self::Target::default();
    {decode}
        Ok(value)
      }}
    }}
    "#,
    encode = fields.iter()
      .map(|(name, _)| format!("    registry.encode(writer, &value.{name})?;"))
      .collect::<Vec<_>>()
      .join("\n"),
    decode = fields.iter()
      .map(|(name, _)| format!("    value.{name} = registry.decode(reader)?;"))
      .collect::<Vec<_>>()
      .join("\n")
  )
}

fn generate_packet(packet_id: i32, name: &str, model_id: i32) -> String {
  formatdoc!(
    r#"
    impl Packet for {name} {{
      fn as_any(&self) -> &dyn Any {{ self }}
      fn as_any_mut(&mut self) -> &mut dyn Any {{ self }}

      fn packet_name(&self) -> &str {{ type_name::<Self>() }}
      fn packet_id(&self) -> i32 {{ {packet_id} }}
      fn model_id(&self) -> i32 {{ {model_id} }}
    }}
    "#
  )
}

#[derive(Debug)]
struct Module {
  name: String,
  children: BTreeMap<String, Module>,
  identifiers: Vec<String>
}

impl Module {
  fn new(name: String) -> Module {
    Module {
      name,
      children: BTreeMap::new(),
      identifiers: Vec::new()
    }
  }

  fn group_identifiers(root: String, identifiers: Vec<String>) -> Module {
    let mut root = Module::new(root);

    for identifier in identifiers {
      let parts: Vec<&str> = identifier.split("::").collect();
      let mut parent_module = &mut root;

      for part in parts.iter().take(parts.len() - 1) {
        if part.is_empty() {
          continue;
        }

        let current_module = parent_module.children.entry(part.to_string())
          .or_insert_with(|| Module::new(part.to_string()));
        parent_module = current_module;
      }

      parent_module.identifiers.push((*parts.last().unwrap()).to_owned());
    }

    root
  }
}

fn indent(string: &str, depth: usize) -> String {
  let indent = "  ".repeat(depth);
  let mut indented = String::with_capacity(string.len());
  for line in string.lines() {
    indented.push_str(&indent);
    indented.push_str(line);
    indented.push('\n');
  }

  indented
}

fn fmt<'a>(
  definitions: &BTreeMap<i32, PacketDefinition>,
  module: &'a Module,
  formatter: &mut String,
  depth: usize,
  path: &mut Vec<&'a Module>
) -> fmt::Result {
  if module.name == "unknown" {
    formatter.push_str(&indent("#[allow(non_camel_case_types)]\n", depth));
  }
  formatter.push_str(&indent(&format!("pub mod {} {{\n", module.name), depth));

  formatter.push_str(&indent("use std::{any::{Any, type_name}, io::{Write, Read}};\n", depth + 1));
  formatter.push_str(&indent("use crate::{packet::{*, enums::*, structs::*}, codec::*};\n", depth + 1));
  formatter.push('\n');

  for child in module.children.values() {
    path.push(child);
    fmt(definitions, child, formatter, depth + 1, path)?;
  }

  for identifier in &module.identifiers {
    let path = path.iter().map(|it| format!("{}.", it.name)).collect::<Vec<_>>().join("") + identifier;
    let (id, definition) = definitions.iter().find(|(_, packet)| packet.name.as_deref() == Some(&path)).unwrap();

    let mut packet = String::new();
    if definition.name.is_none() {
      packet.push_str(&format!("// No name defined for packet {} (model: {})\n", id, definition.model));
      continue;
    }

    writeln!(packet, "/* Packet {}: {} */", id, path)?;
    packet.push_str(&generate_struct(identifier, &definition.fields));
    packet.push_str(&generate_codec(identifier, &definition.fields));
    packet.push_str(&generate_packet(*id, identifier, definition.model));
    packet.push('\n');

    formatter.push_str(&indent(&packet, depth + 1));
  }

  formatter.push_str(&indent("}", depth));
  formatter.push('\n');

  path.pop();

  Ok(())
}

macro_rules! read_file {
  ($file:expr) => {{
    let content = BufReader::new(File::open($file).unwrap());
    serde_yaml::from_reader(content).unwrap()
  }};
}

fn main() {
  println!("cargo:rerun-if-changed=definitions/codecs.yaml");
  println!("cargo:rerun-if-changed=definitions/packets.yaml");

  if env::var("SKIP_PROTOCOL_GEN").unwrap_or("0".to_string()) == "1" {
    return;
  }

  let out_dir = env::var("OUT_DIR").unwrap();

  let mut definitions: BTreeMap<i32, PacketDefinition> = read_file!("definitions/packets.yaml");

  // Generate dummy name for packets without name
  for (id, definition) in definitions.iter_mut() {
    if definition.name.is_none() {
      definition.name = Some(format!("unknown.Packet_{id}").replace("-", "neg"));
    }
  }

  let root_module = Module::group_identifiers("packets".to_owned(), definitions.values()
    .filter_map(|it| it.name.as_ref())
    .map(|it| it.replace('.', "::"))
    .collect::<Vec<_>>());

  let mut out = String::new();
  out.push_str("// This file is automatically @generated.\n");
  out.push_str("// It is not intended for manual editing.\n\n");

  out.push_str("#[allow(unused_imports)]\n\n");

  let mut path = Vec::new();
  fmt(&definitions, &root_module, &mut out, 0, &mut path).unwrap();

  out.push_str("pub(super) mod internal {\n");
  out.push_str("  use crate::packet::PacketRegistry;\n\n");
  out.push_str("  pub fn register_packets(registry: &mut PacketRegistry) {\n");
  for (_, definition) in definitions {
    if let Some(name) = definition.name {
      out.push_str(&format!(
        "    registry.register_packet::<super::packets::{name}>(Default::default());\n",
        name = name.replace('.', "::")
      ));
    }
  }
  out.push_str("  }\n");
  out.push_str("}\n");

  let path = Path::new(&out_dir);
  let mut packets = File::create(path.join("packets.rs")).unwrap();
  packets.write_all(out.as_bytes()).unwrap();
}

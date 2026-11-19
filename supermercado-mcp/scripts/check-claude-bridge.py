"""Verifica el puente configurado en Claude sin cambiar datos del laboratorio."""
import json
import os
from pathlib import Path
import queue
import subprocess
import threading

config_path = Path(os.environ['APPDATA']) / 'Claude' / 'claude_desktop_config.json'
config = json.loads(config_path.read_text(encoding='utf-8-sig'))
entry = config['mcpServers']['tareas-mcp']
messages = queue.Queue()
process = subprocess.Popen([entry['command'], *entry['args']], stdin=subprocess.PIPE,
                           stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                           text=True, encoding='utf-8')

def receive():
    for line in process.stdout:
        try:
            messages.put(json.loads(line))
        except json.JSONDecodeError:
            pass

threading.Thread(target=receive, daemon=True).start()
# Drenar stderr para que los logs del puente no bloqueen el proceso.
threading.Thread(target=lambda: [None for _ in process.stderr], daemon=True).start()

def send(method, params=None, request_id=None):
    payload = {'jsonrpc': '2.0', 'method': method}
    if params is not None:
        payload['params'] = params
    if request_id is not None:
        payload['id'] = request_id
    process.stdin.write(json.dumps(payload) + '\n')
    process.stdin.flush()
    if request_id is None:
        return None
    while True:
        result = messages.get(timeout=30)
        if result.get('id') == request_id:
            if 'error' in result:
                raise RuntimeError(result['error'])
            return result['result']

try:
    send('initialize', {'protocolVersion': '2024-11-05', 'capabilities': {},
                        'clientInfo': {'name': 'lab-bridge-check', 'version': '1.0'}}, 1)
    send('notifications/initialized')
    tools = send('tools/list', {}, 2)['tools']
    expected = {'listarProductos', 'buscarProducto', 'crearPedido', 'agregarProductoPedido',
                'consultarPedido', 'confirmarPedido', 'cancelarPedido'}
    assert len(tools) == 7 and {t['name'] for t in tools} == expected
    result = send('tools/call', {'name': 'listarProductos', 'arguments': {}}, 3)
    assert not result.get('isError'), 'Fallo listarProductos'
    print('Puente STDIO -> HTTP OK: siete herramientas de supermercado y listarProductos.')
finally:
    process.stdin.close()
    try:
        process.wait(timeout=5)
    except subprocess.TimeoutExpired:
        process.terminate()
        process.wait(timeout=5)

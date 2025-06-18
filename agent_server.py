from flask import Flask, request, jsonify

app = Flask(__name__)

@app.route('/agent', methods=['POST'])
def handle_message():
    data = request.json
    print(f"Received message: {data}")
    
    return jsonify({
        "content": f"Echo: {data.get('content')}",
        "type": "response"
    })

if __name__ == '__main__':
    app.run(port=6001)
